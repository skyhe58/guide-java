import {
  flowRendererV2,
  flowStyles
} from "./chunk-J47OPRJ6.js";
import {
  flowDb,
  parser$1
} from "./chunk-3Q7YYSKZ.js";
import "./chunk-4TSPKHNT.js";
import "./chunk-J4YU2UA3.js";
import "./chunk-I4JTSAWL.js";
import "./chunk-NL4EBSVH.js";
import "./chunk-R7D6Y7CH.js";
import {
  require_dist,
  setConfig
} from "./chunk-YJHHM52J.js";
import {
  require_dayjs_min
} from "./chunk-JLOPIRZS.js";
import {
  __toESM
} from "./chunk-PR4QN5HX.js";

// node_modules/.pnpm/mermaid@10.9.1/node_modules/mermaid/dist/flowDiagram-v2-13329dc7.js
var import_dayjs = __toESM(require_dayjs_min(), 1);
var import_sanitize_url = __toESM(require_dist(), 1);
var diagram = {
  parser: parser$1,
  db: flowDb,
  renderer: flowRendererV2,
  styles: flowStyles,
  init: (cnf) => {
    if (!cnf.flowchart) {
      cnf.flowchart = {};
    }
    cnf.flowchart.arrowMarkerAbsolute = cnf.arrowMarkerAbsolute;
    setConfig({ flowchart: { arrowMarkerAbsolute: cnf.arrowMarkerAbsolute } });
    flowRendererV2.setConf(cnf.flowchart);
    flowDb.clear();
    flowDb.setGen("gen-2");
  }
};
export {
  diagram
};
//# sourceMappingURL=flowDiagram-v2-13329dc7-BGQZD72Y.js.map
