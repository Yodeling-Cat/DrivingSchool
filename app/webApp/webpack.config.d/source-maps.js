// The Kotlin/Wasm glue locates its .wasm file through `import.meta.url`, and a webpack bundle that
// wraps every module in `eval(...)` gives that code a non-module scope, which the browser refuses
// with "Cannot use 'import.meta' outside a module" (webpack#17971 is the same failure). Emit real
// source maps instead of eval-wrapped modules, so a development bundle keeps module scope.
config.devtool = "source-map";
config.plugins = (config.plugins || []).filter(
    (plugin) => !plugin || !plugin.constructor || plugin.constructor.name !== "EvalSourceMapDevToolPlugin"
);
