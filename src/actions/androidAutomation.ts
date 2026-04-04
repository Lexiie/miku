import {
  logger,
  type Action,
  type ActionResult,
  type Content,
  type HandlerCallback,
  type IAgentRuntime,
  type Memory,
  type State,
} from "@elizaos/core";
import { parseAndroidCommand } from "../parser";

export const androidAutomationAction: Action = {
  name: "ANDROID_AUTOMATION",
  similes: ["AUTOMATE", "CONTROL_DEVICE", "EXECUTE_ACTION"],
  description: "Parse natural language commands into structured Android automation actions",

  validate: async (_runtime: IAgentRuntime, message: Memory, _state?: State) => {
    return typeof message.content.text === "string" && message.content.text.trim().length > 0;
  },

  handler: async (
    _runtime: IAgentRuntime,
    message: Memory,
    _state?: State,
    _options?: Record<string, unknown>,
    callback?: HandlerCallback
  ): Promise<ActionResult> => {
    const text = typeof message.content.text === "string" ? message.content.text : "";
    const response = parseAndroidCommand(text);

    const responseContent: Content = {
      text: JSON.stringify(response),
      actions: response.actions.map((action) => action.type),
      source: message.content.source,
    };

    if (callback) {
      await callback(responseContent);
    }

    logger.info({ actions: response.actions }, "Parsed Android automation request");

    return {
      text: response.text,
      data: { actions: response.actions },
      values: { actionCount: response.actions.length },
      success: true,
    };
  },

  examples: [
    [
      {
        name: "{{name1}}",
        content: { text: "Set alarm for 7 AM and turn on WiFi" },
      },
      {
        name: "{{name2}}",
        content: {
          text: '{"text":"Alarm ready for 07:00. WiFi will be turned on","actions":[{"type":"SET_ALARM","params":{"hour":7,"minute":0,"label":"Alarm"}},{"type":"TOGGLE_WIFI","params":{"enable":true}}]}',
          actions: ["SET_ALARM", "TOGGLE_WIFI"],
        },
      },
    ],
  ],
};
