from http.server import HTTPServer, BaseHTTPRequestHandler
import os
import sys

ADDR = ('', 8123 if not "--port" in sys.argv else sys.argv[sys.argv.index("--port")+1])

class RequestHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        filename = self.requestline.split(" ")[1].split("/")[-1]
        if not filename in os.listdir("../archive"):
            self.send_response(403)
            self.end_headers()
            return
        with open(f"../archive/{filename}", "rb") as file:
            doc = file.read()
        self.send_response(200)
        self.send_header("Content-Type", "application/pdf")
        self.end_headers()
        self.wfile.write(doc)


http = HTTPServer(ADDR, RequestHandler)

try:
    http.serve_forever()
except KeyboardInterrupt:
    print("\rExiting!")