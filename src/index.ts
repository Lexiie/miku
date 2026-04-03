import { Plugin } from "@elizaos/core";
import { androidAutomationAction } from "./actions/androidAutomation";

export const androidPlugin: Plugin = {
  name: "android-automation",
  description: "Android device automation plugin",
  actions: [androidAutomationAction],
  evaluators: [],
  providers: []
};

export * from "./api";
export default androidPlugin;
