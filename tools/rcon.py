"""极简 RCON 客户端，用来向测试服务端发命令。"""
import socket
import struct
import sys


class Rcon:
    def __init__(self, host="127.0.0.1", port=25575, password="legacytest"):
        self.host = host
        self.port = port
        self.password = password
        self.request_id = 0
        self.sock = socket.create_connection((host, port), timeout=5)
        self.sock.settimeout(3.0)
        self._login()

    def _send(self, packet_type, payload):
        self.request_id += 1
        body = struct.pack("<ii", self.request_id, packet_type) + payload.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(body)) + body)
        return self.request_id

    def _recv_exact(self, count):
        data = b""
        while len(data) < count:
            chunk = self.sock.recv(count - len(data))
            if not chunk:
                raise ConnectionError("RCON connection closed")
            data += chunk
        return data

    def _recv_packet(self):
        (length,) = struct.unpack("<i", self._recv_exact(4))
        body = self._recv_exact(length)
        request_id, _packet_type = struct.unpack("<ii", body[:8])
        payload = body[8:-2].decode("utf-8", "replace")
        return request_id, payload

    def _login(self):
        request_id = self._send(3, self.password)
        try:
            response_id, _ = self._recv_packet()
        except socket.timeout:
            return
        if response_id == -1:
            raise PermissionError("RCON authentication failed")

    def command(self, command):
        request_id = self._send(2, command)
        parts = []
        self.sock.settimeout(4.0)
        try:
            while True:
                response_id, payload = self._recv_packet()
                if response_id == request_id or response_id == -1:
                    parts.append(payload)
                self.sock.settimeout(0.2)
        except socket.timeout:
            pass
        return "".join(parts)


if __name__ == "__main__":
    import os

    command = " ".join(sys.argv[1:])
    client = Rcon(
        host=os.environ.get("RCON_HOST", "127.0.0.1"),
        port=int(os.environ.get("RCON_PORT", "25575")),
        password=os.environ.get("RCON_PASS", "legacytest"),
    )
    print(client.command(command))
