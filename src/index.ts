import { Plugin } from "@elizaos/core";
import { androidAutomationAction } from "./actions/androidAutomation";
import { androidApiRoutes } from "./api";

export const androidPlugin: Plugin = {
  name: "android-automation",
  description: "Android device automation plugin",
  actions: [androidAutomationAction],
  routes: androidApiRoutes,
  evaluators: [],
  providers: []
};

export * from "./api";
export * from "./parser";
export default androidPlugin;
