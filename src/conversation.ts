import { parseAndroidCommand, type AndroidChatResponse } from "./parser.ts";

const SYSTEM_PROMPT =
  "You are Miku, a concise and helpful mobile assistant. Reply naturally for normal conversation. Keep replies short (1-3 sentences). Do not claim you executed Android actions unless explicitly provided by the system.";
const TOTAL_FALLBACK_TIMEOUT_MS = 15000;
const GEMINI_FALLBACK_RESERVE_MS = 4000;

function normalizeBaseUrl(url: string): string {
  return url.endsWith("/") ? url.slice(0, -1) : url;
}

async function fetchWithTimeout(url: string, init: RequestInit, timeoutMs = 15000): Promise<Response> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    return await fetch(url, {
      ...init,
      signal: controller.signal,
    });
  } finally {
    clearTimeout(timer);
  }
}

async function fetchOpenAIReply(userText: string, timeoutMs: number): Promise<string | null> {
  if (timeoutMs <= 0) {
    return null;
  }

  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    return null;
  }

  const baseUrl = normalizeBaseUrl(process.env.OPENAI_BASE_URL ?? "https://api.openai.com/v1");
  const model = process.env.OPENAI_LARGE_MODEL ?? process.env.OPENAI_MODEL ?? "gpt-4.1-mini";

  const response = await fetchWithTimeout(`${baseUrl}/chat/completions`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model,
      temperature: 0.6,
      max_tokens: 220,
      messages: [
        { role: "system", content: SYSTEM_PROMPT },
        { role: "user", content: userText },
      ],
    }),
  }, timeoutMs);

  if (!response.ok) {
    void response.body?.cancel();
    return null;
  }

  let data: {
    choices?: Array<{ message?: { content?: string | Array<{ type?: string; text?: string }> } }>;
  };

  try {
    data = (await response.json()) as {
      choices?: Array<{ message?: { content?: string | Array<{ type?: string; text?: string }> } }>;
    };
  } catch {
    return null;
  }

  const content = data.choices?.[0]?.message?.content;
  if (typeof content === "string") {
    return content.trim() || null;
  }

  if (Array.isArray(content)) {
    const text = content
      .map((part) => (part.type === "text" ? part.text : null))
      .filter((part): part is string => Boolean(part))
      .join("\n")
      .trim();
    return text || null;
  }

  return null;
}

async function fetchGeminiReply(userText: string, timeoutMs: number): Promise<string | null> {
  if (timeoutMs <= 0) {
    return null;
  }

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    return null;
  }

  const model = process.env.GEMINI_MODEL ?? "gemini-2.5-flash";
  const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`;

  const response = await fetchWithTimeout(endpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": apiKey,
    },
    body: JSON.stringify({
      systemInstruction: {
        parts: [{ text: SYSTEM_PROMPT }],
      },
      contents: [
        {
          role: "user",
          parts: [{ text: userText }],
        },
      ],
      generationConfig: {
        temperature: 0.6,
        maxOutputTokens: 220,
      },
    }),
  }, timeoutMs);

  if (!response.ok) {
    void response.body?.cancel();
    return null;
  }

  let data: {
    candidates?: Array<{
      content?: {
        parts?: Array<{ text?: string }>;
      };
    }>;
  };

  try {
    data = (await response.json()) as {
      candidates?: Array<{
        content?: {
          parts?: Array<{ text?: string }>;
        };
      }>;
    };
  } catch {
    return null;
  }

  const text = data.candidates?.[0]?.content?.parts
    ?.map((part) => part.text?.trim() ?? "")
    .filter(Boolean)
    .join("\n")
    .trim();

  return text || null;
}

async function generateConversationalReply(userText: string): Promise<string | null> {
  const startedAt = Date.now();
  const remainingBudget = () => TOTAL_FALLBACK_TIMEOUT_MS - (Date.now() - startedAt);
  const openAiBudget = Math.max(0, TOTAL_FALLBACK_TIMEOUT_MS - GEMINI_FALLBACK_RESERVE_MS);

  try {
    const openAiReply = await fetchOpenAIReply(userText, openAiBudget);
    if (openAiReply) {
      return openAiReply;
    }
  } catch {
    // Continue to Gemini fallback if OpenAI call fails.
  }

  try {
    const geminiReply = await fetchGeminiReply(userText, remainingBudget());
    if (geminiReply) {
      return geminiReply;
    }
  } catch {
    return null;
  }

  return null;
}

export async function buildChatResponse(text: string): Promise<AndroidChatResponse> {
  const trimmedText = text.trim();
  const parsed = parseAndroidCommand(trimmedText);
  if (trimmedText.length === 0) {
    return parsed;
  }

  if (parsed.actions.length > 0) {
    return parsed;
  }

  const conversationalReply = await generateConversationalReply(trimmedText);
  if (!conversationalReply) {
    return parsed;
  }

  return {
    text: conversationalReply,
    actions: [],
  };
}
