/** @type {import('dependency-cruiser').IConfiguration} */
module.exports = {
  forbidden: [
    {
      name: "cy-no-circular",
      comment:
        "Circular dependencies make module load order unpredictable and hide coupling; no cycle may exist in the module graph.",
      severity: "error",
      from: { pathNot: "\\.(test|spec)\\.[tj]sx?$" },
      to: { circular: true },
    },

    {
      name: "mb-ui-primitives-are-leaves",
      comment:
        "src/components/ui/ is the in-house component library's stand-in. It may reach for cn() in src/lib/ and for its siblings, and nothing else — a primitive that imports a page or a feature cannot be swapped out for the published package.",
      severity: "error",
      from: { path: "^src/components/ui/", pathNot: "\\.(test|spec)\\.[tj]sx?$" },
      to: { path: "^src/", pathNot: "^src/(components/ui|lib)/" },
    },

    {
      name: "mb-lib-is-a-leaf",
      comment:
        "src/lib/ holds framework-agnostic helpers every layer may call. It must not import back out of itself, or the dependency direction stops being one-way.",
      severity: "error",
      from: { path: "^src/lib/", pathNot: "\\.(test|spec)\\.[tj]sx?$" },
      to: { path: "^src/", pathNot: "^src/lib/" },
    },

    {
      name: "mb-no-top-level-catchall-dirs",
      comment:
        "There is no src/types, src/hooks or src/utils. Types live in the folder that owns them; shared helpers and hooks live in src/lib (which is what components.json points the shadcn CLI at).",
      severity: "error",
      from: {},
      to: { path: "^src/(types|hooks|utils)/" },
    },

    {
      name: "lb-no-css-in-logic",
      comment:
        "Helpers must not import stylesheets; style imports belong in component files and in the composition root.",
      severity: "error",
      from: { path: "^src/lib/" },
      to: { path: "\\.(css|scss|sass|less)$" },
    },

    {
      name: "cq-no-devdep-in-prod",
      comment:
        "Production source must not import devDependencies; they are absent at runtime. *.testHelpers.ts(x) is exempt as the shared-test-support naming convention — it is excluded from the production TypeScript project the same way *.test.ts(x) is (tsconfig.json), and is deliberately not named *.helpers.ts(x), which stays inside this rule.",
      severity: "error",
      from: {
        path: "^src/",
        pathNot: ["\\.(test|spec)\\.[tj]sx?$", "\\.testHelpers\\.[tj]sx?$"],
      },
      to: { dependencyTypes: ["npm-dev"] },
    },
  ],

  options: {
    doNotFollow: {
      path: "node_modules",
    },

    // node_modules is deliberately in scope. `doNotFollow` already stops the
    // crawl at the package boundary, so this adds leaf nodes only — but those
    // leaves are what carry the `npm` / `npm-dev` dependency types. Narrow
    // this to "^src/" and cq-no-devdep-in-prod stops matching anything and
    // passes vacuously.
    includeOnly: ["^src/", "^node_modules/"],

    // Also how the `@/*` path alias gets resolved; without it every aliased
    // import reads as unresolvable and no rule above matches on it.
    tsConfig: { fileName: "./tsconfig.json" },
    tsPreCompilationDeps: true,

    moduleSystems: ["es6", "cjs"],

    reporterOptions: {
      dot: {
        collapsePattern: "^node_modules/[^/]+/",
      },
      archi: {
        collapsePattern: "^(node_modules|src/[^/]+/[^/]+)/",
      },
    },
  },
};
