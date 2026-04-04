export type AndroidActionType =
  | "SET_ALARM"
  | "SET_TIMER"
  | "ADD_CALENDAR"
  | "GET_EVENTS"
  | "SEND_SMS"
  | "MAKE_CALL"
  | "OPEN_APP"
  | "UNINSTALL_APP"
  | "TOGGLE_WIFI"
  | "TOGGLE_BLUETOOTH"
  | "SET_BRIGHTNESS"
  | "TOGGLE_FLASHLIGHT"
  | "SET_RINGER_MODE"
  | "SET_VOLUME"
  | "SEND_NOTIFICATION"
  | "SET_REMINDER"
  | "GET_LOCATION";

export interface AndroidAction {
  type: AndroidActionType;
  params: Record<string, unknown>;
}

export interface AndroidChatRequest {
  text: string;
  userId?: string;
}

export interface AndroidChatResponse {
  text: string;
  actions: AndroidAction[];
}

const SUPPORTED_CAPABILITIES =
  "I can help with alarms, timers, calendar events, SMS, calls, apps, WiFi, Bluetooth, flashlight, brightness, volume, reminders, notifications, and location.";

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function extractNumber(text: string): number | null {
  const match = text.match(/\b(\d{1,3})\b/);
  return match ? Number.parseInt(match[1], 10) : null;
}

function extractPhoneNumber(text: string): string | null {
  const match = text.match(/\+?[\d\s-]{10,}/);
  return match ? match[0].replace(/[\s-]+/g, "") : null;
}

function extractCommandTarget(text: string, command: string): string | null {
  const regex = new RegExp(`(?:^|\\b(?:and|then)\\b)\\s*${command}\\s+(.+?)(?:\\s+\\b(?:and|then)\\b|$)`, "i");
  const match = text.match(regex);
  if (!match) {
    return null;
  }

  return match[1].trim().replace(/[.!?]+$/, "") || null;
}

function extractQuotedOrTrailingText(text: string, patterns: RegExp[]): string | null {
  for (const pattern of patterns) {
    const match = text.match(pattern);
    if (match?.[1]) {
      return match[1].trim().replace(/^"|"$/g, "");
    }
  }

  return null;
}

function extractDurationMs(text: string): number | null {
  const durationPattern = /(\d+)\s*(hours?|hrs?|hr|minutes?|mins?|min|seconds?|secs?|sec)/gi;
  let totalMs = 0;
  let matched = false;

  for (const match of text.matchAll(durationPattern)) {
    matched = true;
    const value = Number.parseInt(match[1], 10);
    const unit = match[2].toLowerCase();

    if (unit.startsWith("hour") || unit === "hr" || unit === "hrs") {
      totalMs += value * 60 * 60 * 1000;
    } else if (unit.startsWith("min")) {
      totalMs += value * 60 * 1000;
    } else {
      totalMs += value * 1000;
    }
  }

  return matched ? totalMs : null;
}

function parseDateContext(text: string): Date {
  const now = new Date();
  const date = new Date(now);

  if (text.includes("tomorrow")) {
    date.setDate(date.getDate() + 1);
  }

  return date;
}

function extractTime(text: string): { hour: number; minute: number } | null {
  const match = text.match(/\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b/i);
  if (!match) {
    return null;
  }

  let hour = Number.parseInt(match[1], 10);
  const minute = match[2] ? Number.parseInt(match[2], 10) : 0;
  const meridiem = match[3]?.toLowerCase();

  if (meridiem === "pm" && hour < 12) {
    hour += 12;
  }

  if (meridiem === "am" && hour === 12) {
    hour = 0;
  }

  if (!meridiem && hour <= 7 && text.includes("evening")) {
    hour += 12;
  }

  return { hour: clamp(hour, 0, 23), minute: clamp(minute, 0, 59) };
}

function buildIsoDate(text: string, fallbackHour: number, fallbackMinute: number): string {
  const base = parseDateContext(text);
  const time = extractTime(text) ?? { hour: fallbackHour, minute: fallbackMinute };

  base.setHours(time.hour, time.minute, 0, 0);
  return base.toISOString();
}

function pushAction(actions: AndroidAction[], action: AndroidAction | null | undefined) {
  if (!action) {
    return;
  }

  const duplicate = actions.some(
    (existing) => existing.type === action.type && JSON.stringify(existing.params) === JSON.stringify(action.params)
  );

  if (!duplicate) {
    actions.push(action);
  }
}

function inferToggleValue(text: string, positiveHints: string[], negativeHints: string[], defaultValue = true): boolean {
  if (negativeHints.some((hint) => text.includes(hint))) {
    return false;
  }

  if (positiveHints.some((hint) => text.includes(hint))) {
    return true;
  }

  return defaultValue;
}

function buildSummary(actions: AndroidAction[]): string {
  if (actions.length === 0) {
    return SUPPORTED_CAPABILITIES;
  }

  const messages = actions.map((action) => {
    switch (action.type) {
      case "SET_ALARM": {
        const hour = Number(action.params.hour ?? 7);
        const minute = Number(action.params.minute ?? 0);
        return `Alarm ready for ${hour.toString().padStart(2, "0")}:${minute.toString().padStart(2, "0")}`;
      }
      case "SET_TIMER":
        return "Timer prepared";
      case "ADD_CALENDAR":
        return `Calendar event \"${String(action.params.title ?? "Event")}\" prepared`;
      case "GET_EVENTS":
        return `Checking calendar for ${String(action.params.date ?? "today")}`;
      case "SEND_SMS":
        return `SMS ready for ${String(action.params.phone ?? "recipient")}`;
      case "MAKE_CALL":
        return `Call ready for ${String(action.params.phone ?? action.params.target ?? "contact")}`;
      case "OPEN_APP":
        return `Opening ${String(action.params.appName ?? "app")}`;
      case "UNINSTALL_APP":
        return `Preparing to uninstall ${String(action.params.appName ?? "app")}`;
      case "TOGGLE_WIFI":
        return `WiFi will be turned ${action.params.enable ? "on" : "off"}`;
      case "TOGGLE_BLUETOOTH":
        return `Bluetooth will be turned ${action.params.enable ? "on" : "off"}`;
      case "SET_BRIGHTNESS":
        return `Brightness set to ${String(action.params.level ?? 50)}%`;
      case "TOGGLE_FLASHLIGHT":
        return `Flashlight will be turned ${action.params.enable ? "on" : "off"}`;
      case "SET_RINGER_MODE":
        return `Phone will switch to ${String(action.params.mode ?? "normal")} mode`;
      case "SET_VOLUME":
        return `Volume set to ${String(action.params.level ?? 50)}%`;
      case "SEND_NOTIFICATION":
        return "Notification ready";
      case "SET_REMINDER":
        return "Reminder scheduled";
      case "GET_LOCATION":
        return "Fetching your location";
      default:
        return action.type;
    }
  });

  return messages.join(". ");
}

export function parseAndroidCommand(text: string): AndroidChatResponse {
  const originalText = text.trim();
  const lowerText = originalText.toLowerCase();
  const actions: AndroidAction[] = [];

  if (!originalText) {
    return { text: "Text is required", actions: [] };
  }

  if (lowerText.includes("alarm")) {
    const time = extractTime(lowerText) ?? { hour: 7, minute: 0 };
    pushAction(actions, {
      type: "SET_ALARM",
      params: {
        hour: time.hour,
        minute: time.minute,
        label: "Alarm",
      },
    });
  }

  if (lowerText.includes("timer")) {
    pushAction(actions, {
      type: "SET_TIMER",
      params: {
        duration: extractDurationMs(lowerText) ?? 5 * 60 * 1000,
      },
    });
  }

  if (lowerText.includes("wifi")) {
    pushAction(actions, {
      type: "TOGGLE_WIFI",
      params: {
        enable: inferToggleValue(lowerText, ["turn on", "enable", "connect"], ["turn off", "disable"], true),
      },
    });
  }

  if (lowerText.includes("bluetooth")) {
    pushAction(actions, {
      type: "TOGGLE_BLUETOOTH",
      params: {
        enable: inferToggleValue(lowerText, ["turn on", "enable"], ["turn off", "disable"], true),
      },
    });
  }

  if (lowerText.includes("flashlight") || lowerText.includes("torch")) {
    pushAction(actions, {
      type: "TOGGLE_FLASHLIGHT",
      params: {
        enable: inferToggleValue(lowerText, ["turn on", "enable"], ["turn off", "disable"], true),
      },
    });
  }

  if (lowerText.includes("brightness")) {
    pushAction(actions, {
      type: "SET_BRIGHTNESS",
      params: {
        level: clamp(extractNumber(lowerText) ?? 50, 0, 100),
      },
    });
  }

  if (lowerText.includes("volume")) {
    pushAction(actions, {
      type: "SET_VOLUME",
      params: {
        level: clamp(extractNumber(lowerText) ?? 50, 0, 100),
        stream: lowerText.includes("alarm") ? "alarm" : lowerText.includes("ring") ? "ring" : "music",
      },
    });
  }

  if (lowerText.includes("silent") || lowerText.includes("vibrate") || lowerText.includes("ringer") || lowerText.includes("ring mode")) {
    let mode = "normal";
    if (lowerText.includes("silent")) {
      mode = "silent";
    } else if (lowerText.includes("vibrate")) {
      mode = "vibrate";
    }

    pushAction(actions, {
      type: "SET_RINGER_MODE",
      params: { mode },
    });
  }

  if (lowerText.includes("calendar") && (lowerText.includes("what") || lowerText.includes("today") || lowerText.includes("schedule"))) {
    pushAction(actions, {
      type: "GET_EVENTS",
      params: {
        date: lowerText.includes("tomorrow") ? "tomorrow" : "today",
      },
    });
  } else if (
    lowerText.includes("calendar") ||
    lowerText.includes("meeting") ||
    lowerText.includes("event")
  ) {
    const title = extractQuotedOrTrailingText(originalText, [
      /(?:add|create|schedule)\s+(.+?)\s+(?:tomorrow|today|at|on)\b/i,
      /(?:meeting|event)\s+with\s+(.+?)\s+(?:tomorrow|today|at|on)\b/i,
    ]) ?? "Event";

    const start = buildIsoDate(lowerText, 9, 0);
    const end = new Date(start);
    end.setHours(end.getHours() + 1);

    pushAction(actions, {
      type: "ADD_CALENDAR",
      params: {
        title,
        start,
        end: end.toISOString(),
      },
    });
  }

  const smsPhone = extractPhoneNumber(originalText);
  if (lowerText.includes("sms") || (lowerText.includes("text") && Boolean(smsPhone))) {
    const message = extractQuotedOrTrailingText(originalText, [
      /(?:saying|message)\s+(.+)/i,
      /:\s*(.+)$/i,
    ]);

    if (smsPhone && message) {
      pushAction(actions, {
        type: "SEND_SMS",
        params: {
          phone: smsPhone,
          message,
        },
      });
    }
  }

  const callTarget = extractCommandTarget(originalText, "call");
  if (callTarget) {
    pushAction(actions, {
      type: "MAKE_CALL",
      params: smsPhone ? { phone: smsPhone } : { target: callTarget },
    });
  }

  const appToOpen = extractCommandTarget(originalText, "open");
  if (appToOpen) {
    pushAction(actions, {
      type: "OPEN_APP",
      params: { appName: appToOpen },
    });
  }

  const appToUninstall = extractCommandTarget(originalText, "uninstall");
  if (appToUninstall) {
    pushAction(actions, {
      type: "UNINSTALL_APP",
      params: { appName: appToUninstall },
    });
  }

  if (lowerText.includes("where am i") || lowerText.includes("location")) {
    pushAction(actions, {
      type: "GET_LOCATION",
      params: {},
    });
  }

  if (lowerText.includes("remind me") || lowerText.includes("set reminder") || lowerText.includes("reminder")) {
    const delayMs = extractDurationMs(lowerText);
    const reminderMessage = extractQuotedOrTrailingText(originalText, [
      /remind me to\s+(.+?)(?:\s+in\b|\s+at\b|\s+and\b|\s+then\b|$)/i,
      /remind me that\s+(.+?)(?:\s+in\b|\s+at\b|\s+and\b|\s+then\b|$)/i,
      /set reminder\s+(.+?)(?:\s+in\b|\s+at\b|\s+and\b|\s+then\b|$)/i,
    ]) ?? "Reminder";

    pushAction(actions, {
      type: "SET_REMINDER",
      params: {
        message: reminderMessage,
        delayMs: delayMs ?? 30 * 60 * 1000,
        triggerAt: delayMs ? undefined : buildIsoDate(lowerText, 17, 0),
      },
    });
  } else if (lowerText.includes("notify") || lowerText.includes("notification")) {
    const body = extractQuotedOrTrailingText(originalText, [
      /notify(?: me| that)?\s+(.+)/i,
      /notification\s+(.+)/i,
    ]) ?? "Notification";

    pushAction(actions, {
      type: "SEND_NOTIFICATION",
      params: {
        title: "Reminder",
        body,
      },
    });
  }

  return {
    text: buildSummary(actions),
    actions,
  };
}
