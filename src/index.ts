import { type Plugin, type Project, type ProjectAgent } from "@elizaos/core";
import { androidAutomationAction } from "./actions/androidAutomation";
import { androidApiRoutes } from "./api";
import { character } from "./character";

/**
 * Plugin surface exposed to ElizaOS.
 */
export const androidPlugin: Plugin = {
  name: "android-automation",
  description: "Android device automation plugin",
  actions: [androidAutomationAction],
  routes: androidApiRoutes,
  evaluators: [],
  providers: []
};

/**
 * Single project agent wiring for this repo.
 */
export const projectAgent: ProjectAgent = {
  character,
  plugins: [androidPlugin]
};

/**
 * Project entrypoint consumed by ElizaOS CLI/runtime.
 */
const project: Project = {
  agents: [projectAgent]
};

export * from "./api";
export * from "./character";
export * from "./parser";
export default project;
