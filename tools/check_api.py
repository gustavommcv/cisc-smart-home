#!/usr/bin/env python3
"""Checks a running maquete against the API contract in docs/api.md.

The computer must already be connected to the Wi-Fi network of the board; the script
only talks HTTP and changes nothing else. It uses only the Python standard library.

    python3 tools/check_api.py [--host 192.168.4.1] [--quick]

The one command it sends that changes the board is setting the dehumidifier mode, and
it puts the mode back to automatic at the end.
"""
import argparse
import http.client
import json
import select
import socket
import sys
import time

results = []


def check(name, ok, detail=""):
    results.append(ok)
    print(f"[{'PASS' if ok else 'FAIL'}] {name}" + (f"  ({detail})" if detail and not ok else ""))
    return ok


def call(host, method, path, body=None, headers=None):
    """Returns (status, parsed JSON or None, seconds)."""
    connection = http.client.HTTPConnection(host, timeout=8)
    started = time.monotonic()
    connection.request(method, path, body=body, headers=headers or {})
    response = connection.getresponse()
    raw = response.read().decode("utf-8", "replace")
    elapsed = time.monotonic() - started
    connection.close()
    try:
        return response.status, json.loads(raw), elapsed
    except ValueError:
        return response.status, None, elapsed


def error_code(payload):
    return (payload or {}).get("error", {}).get("code")


def entities(home):
    return {e["id"]: e for a in home["areas"] for d in a["devices"] for e in d["entities"]}


class EventClient:
    """One connection to the event stream, read by hand so it can stay open.

    It uses select() instead of socket timeouts: after a timeout Python marks the
    socket as unusable, and a quiet stream is exactly what has to be waited on.
    """

    def __init__(self, host):
        name, _, port = host.partition(":")
        self.sock = socket.create_connection((name, int(port or 80)), timeout=8)
        request = (f"GET /api/v1/events HTTP/1.1\r\nHost: {host}\r\n"
                   "Accept: text/event-stream\r\nConnection: keep-alive\r\n\r\n")
        self.sock.sendall(request.encode())
        self.buffer = b""
        head = self._read_until(b"\r\n\r\n", 5)
        self.status = int(head.split(b" ")[1]) if head else 0

    def _fill(self, seconds):
        """Reads whatever arrives within `seconds`; returns False when the peer closed."""
        ready, _, _ = select.select([self.sock], [], [], max(seconds, 0))
        if not ready:
            return True
        data = self.sock.recv(4096)
        self.buffer += data
        return bool(data)

    def _read_until(self, marker, seconds):
        deadline = time.monotonic() + seconds
        while marker not in self.buffer and time.monotonic() < deadline:
            if not self._fill(deadline - time.monotonic()):
                break
        head, _, self.buffer = self.buffer.partition(marker)
        return head

    def read_events(self, seconds):
        """Names of the events received in `seconds`, in order."""
        names, deadline = [], time.monotonic() + seconds
        while time.monotonic() < deadline:
            alive = self._fill(min(deadline - time.monotonic(), 0.5))
            while b"\n" in self.buffer:
                line, _, self.buffer = self.buffer.partition(b"\n")
                text = line.decode("utf-8", "replace").strip()
                if text.startswith("event:"):
                    names.append(text.split(":", 1)[1].strip())
            if not alive:
                break
        return names

    def close(self):
        self.sock.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--host", default="192.168.4.1")
    parser.add_argument("--quick", action="store_true", help="skip the 18 s wait for the keepalive ping")
    args = parser.parse_args()
    host = args.host

    print(f"Checking http://{host}/api/v1 ...\n")
    status, home, elapsed = call(host, "GET", "/api/v1/home")
    if not check("GET /home answers 200 with JSON", status == 200 and home is not None, f"status {status}"):
        print("\nCannot continue without the home. Is this computer on the board's Wi-Fi?")
        return 1
    boot_id = home.get("bootId")
    found = entities(home)
    check("home has the version, the boot id and both rooms",
          home.get("apiVersion") == 1 and bool(boot_id) and [a["id"] for a in home["areas"]] == ["kitchen", "bedroom"])
    check("home has the eight entities of the maquete", len(found) == 8, f"found {sorted(found)}")
    check("only the dehumidifier is commandable",
          {i for i, e in found.items() if e.get("commandable")} == {"dehumidifier-power", "dehumidifier-mode"})
    print(f"      (home answered in {elapsed * 1000:.0f} ms, boot id {boot_id})")

    status, payload, _ = call(host, "GET", "/api/v1/history")
    check("GET /history answers 200 with a list", status == 200 and isinstance(payload and payload.get("samples"), list), f"status {status}")
    status, payload, _ = call(host, "GET", "/api/v1/history?limit=0")
    check("history with limit=0 is invalid-query", status == 400 and error_code(payload) == "invalid-query", f"status {status}")

    status, payload, _ = call(host, "GET", "/api/v1/nothing")
    check("unknown route is 404 not-found", status == 404 and error_code(payload) == "not-found", f"status {status}")
    status, payload, _ = call(host, "POST", "/api/v1/home", body="{}")
    check("POST on home is 405", status == 405 and error_code(payload) == "method-not-allowed", f"status {status}")

    put = lambda entity, body: call(host, "PUT", f"/api/v1/entities/{entity}/value", body=body,
                                    headers={"Content-Type": "application/json"})
    status, payload, _ = put("valve-open", '{"value": true}')
    check("a read-only entity is 403 not-commandable", status == 403 and error_code(payload) == "not-commandable", f"status {status}")
    status, payload, _ = put("no-such-thing", '{"value": true}')
    check("an unknown entity is 404", status == 404 and error_code(payload) == "not-found", f"status {status}")
    status, payload, _ = put("dehumidifier-power", "not json")
    check("a bad body is invalid-body", status == 400 and error_code(payload) == "invalid-body", f"status {status}")
    status, payload, _ = put("dehumidifier-power", '{"value": "yes"}')
    check("a value of the wrong type is invalid-value", status == 400 and error_code(payload) == "invalid-value", f"status {status}")

    status, payload, _ = put("dehumidifier-mode", '{"value": "manual"}')
    check("setting the mode to manual answers the entity", status == 200 and (payload or {}).get("value") == "manual", f"status {status}")
    _, home_after, _ = call(host, "GET", "/api/v1/home")
    check("the home shows the new mode", entities(home_after)["dehumidifier-mode"]["value"] == "manual")
    status, payload, _ = put("dehumidifier-mode", '{"value": "automatic"}')
    check("the mode goes back to automatic", status == 200 and (payload or {}).get("value") == "automatic", f"status {status}")

    status, payload, _ = call(host, "GET", "/api/v1/events")
    check("the event route without Accept is 406", status == 406 and error_code(payload) == "not-acceptable", f"status {status}")

    stream = EventClient(host)
    check("the event stream opens with 200", stream.status == 200, f"status {stream.status}")
    first = stream.read_events(3)
    check("a new client gets the whole state first", first[:1] == ["home"], f"got {first}")
    if args.quick:
        print("[SKIP] keepalive ping (--quick)")
    else:
        print("      (waiting up to 18 s for the keepalive ping; keep the maquete quiet)")
        later = stream.read_events(18)
        check("a quiet stream sends a ping", "ping" in later, f"got {later}")

    extra = [EventClient(host), EventClient(host)]
    status, payload, _ = call(host, "GET", "/api/v1/events", headers={"Accept": "text/event-stream"})
    check("a fourth listener is refused with 503", status == 503 and error_code(payload) == "too-many-clients", f"status {status}")
    for client in [stream] + extra:
        client.close()

    time.sleep(1)
    status, home_end, _ = call(host, "GET", "/api/v1/home")
    check("the board still answers after all this", status == 200, f"status {status}")
    check("the board did not restart during the test", home_end and home_end.get("bootId") == boot_id,
          f"boot id {boot_id} became {home_end and home_end.get('bootId')}")

    failed = results.count(False)
    print(f"\n{len(results) - failed} passed, {failed} failed")
    return 1 if failed else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, http.client.HTTPException) as error:
        passed = results.count(True)
        print(f"\nThe board stopped answering: {error}")
        print("Is this computer on the Wi-Fi of the board? If the board was answering a moment ago, it may have")
        print("restarted: look at its serial output.")
        print(f"{passed} checks passed before that.")
        sys.exit(2)
