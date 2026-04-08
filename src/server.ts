import { createServer, type IncomingMessage, type ServerResponse } from "node:http";
import { parseAndroidCommand } from "./parser.ts";

const port = Number.parseInt(process.env.SERVER_PORT ?? "3000", 10) || 3000;

type ChatRequest = {
  text?: unknown;
  userId?: unknown;
};

function setCorsHeaders(res: ServerResponse) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");
}

function sendJson(res: ServerResponse, statusCode: number, payload: unknown) {
  setCorsHeaders(res);
  res.statusCode = statusCode;
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  res.end(JSON.stringify(payload));
}

async function readJsonBody(req: IncomingMessage): Promise<ChatRequest> {
  const chunks: Buffer[] = [];

  for await (const chunk of req) {
    chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
  }

  if (chunks.length == 0) {
    return {};
  }

  const raw = Buffer.concat(chunks).toString("utf8");
  return JSON.parse(raw) as ChatRequest;
}

const server = createServer(async (req, res) => {
  const method = req.method ?? "GET";
  const url = new URL(req.url ?? "/", `http://${req.headers.host ?? "localhost"}`);
  const pathname = url.pathname;

  if (method === "OPTIONS") {
    setCorsHeaders(res);
    res.statusCode = 204;
    res.end();
    return;
  }

  if (method === "GET" && (pathname === "/health" || pathname === "/api/health")) {
    sendJson(res, 200, {
      status: "ok",
      timestamp: new Date().toISOString(),
      service: "miku-android-automation",
    });
    return;
  }

  if (method === "POST" && pathname === "/api/chat") {
    try {
      const body = await readJsonBody(req);

      if (typeof body.text !== "string" || body.text.trim() === "") {
        sendJson(res, 400, {
          text: "Text is required",
          actions: [],
        });
        return;
      }

      const response = parseAndroidCommand(body.text);
      sendJson(res, 200, response);
      return;
    } catch (error) {
      sendJson(res, 500, {
        text: `Error processing request: ${error instanceof Error ? error.message : String(error)}`,
        actions: [],
      });
      return;
    }
  }

  if (method === "GET" && pathname === "/") {
    sendJson(res, 200, {
      name: "Miku",
      status: "ok",
      routes: ["/health", "/api/health", "/api/chat"],
    });
    return;
  }

  sendJson(res, 404, {
    error: {
      message: "API endpoint not found",
      code: 404,
    },
  });
});

server.listen(port, "0.0.0.0", () => {
  console.log(`Miku server listening on port ${port}`);
});
