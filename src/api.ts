import { logger, type Route, type RouteRequest, type RouteResponse } from "@elizaos/core";
import { type AndroidChatRequest } from "./parser";
import { buildChatResponse } from "./conversation";

function readChatRequest(body: unknown): AndroidChatRequest | null {
  if (!body || typeof body !== "object") {
    return null;
  }

  const candidate = body as Record<string, unknown>;
  if (typeof candidate.text !== "string") {
    return null;
  }

  return {
    text: candidate.text,
    userId: typeof candidate.userId === "string" ? candidate.userId : undefined,
  };
}

async function handleChat(req: RouteRequest, res: RouteResponse) {
  const chatRequest = readChatRequest(req.body);
  if (!chatRequest) {
    res.status(400).json({
      text: "Text is required",
      actions: [],
    });
    return;
  }

  try {
    const response = await buildChatResponse(chatRequest.text);
    res.json(response);
  } catch (error) {
    logger.error({ error }, "Failed to handle Android chat request");
    res.status(500).json({
      text: "Error processing request",
      actions: [],
    });
  }
}

function handleHealth(_req: RouteRequest, res: RouteResponse) {
  res.json({
    status: "ok",
    timestamp: Date.now(),
  });
}

export const androidApiRoutes: Route[] = [
  {
    name: "android-chat",
    path: "/api/chat",
    type: "POST",
    handler: handleChat,
  },
  {
    name: "android-health",
    path: "/health",
    type: "GET",
    handler: async (req, res) => handleHealth(req, res),
  },
  {
    name: "android-api-health",
    path: "/api/health",
    type: "GET",
    handler: async (req, res) => handleHealth(req, res),
  },
];
