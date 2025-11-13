const sampleSvg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 720 1280">
  <defs>
    <linearGradient id="bg" x1="0%" x2="100%" y1="0%" y2="100%">
      <stop offset="0%" stop-color="#0f172a" />
      <stop offset="100%" stop-color="#020617" />
    </linearGradient>
  </defs>
  <rect width="720" height="1280" fill="url(#bg)" />
  <g stroke="#38bdf8" stroke-width="4">
    <line x1="120" y1="200" x2="600" y2="240" />
    <line x1="80" y1="400" x2="640" y2="420" />
    <line x1="160" y1="760" x2="560" y2="820" />
  </g>
  <g stroke="#f472b6" stroke-width="3">
    <polyline points="120,980 240,900 480,1020 620,880" fill="none" />
  </g>
  <text x="50%" y="92%" text-anchor="middle" fill="#f8fafc" font-size="48" font-family="'Segoe UI', sans-serif">
    Edge Preview
  </text>
</svg>`;

const sampleFrameUri = `data:image/svg+xml;base64,${btoa(sampleSvg)}`;

 type FrameStats = {
  fps: number;
  resolution: { width: number; height: number };
  format: string;
  frameId: number;
};

 type ViewerState = {
  stats: FrameStats;
  mode: "edge" | "raw";
  frameUri: string;
};

 const state: ViewerState = {
  stats: {
    fps: 14.2,
    resolution: { width: 1280, height: 720 },
    format: "RGBA8888",
    frameId: 1,
  },
  mode: "edge",
  frameUri: sampleFrameUri,
};

 type DomRefs = {
  frame: HTMLImageElement;
  fps: HTMLElement;
  resolution: HTMLElement;
  format: HTMLElement;
  frameField: HTMLTextAreaElement;
  log: HTMLElement;
  frameId: HTMLElement;
  modeLabel: HTMLElement;
};

 const refs: Partial<DomRefs> = {};

 const fmtResolution = (stats: FrameStats) => `${stats.resolution.width} x ${stats.resolution.height}`;

 const log = (message: string) => {
  if (!refs.log) return;
  const timestamp = new Date().toLocaleTimeString();
  const entry = document.createElement("p");
  entry.textContent = `[${timestamp}] ${message}`;
  refs.log.prepend(entry);
  while (refs.log.childElementCount > 6) {
    refs.log.removeChild(refs.log.lastElementChild!);
  }
};

 const updateStatsPanel = () => {
  if (!refs.fps || !refs.resolution || !refs.format || !refs.frameId) return;
  refs.fps.textContent = state.stats.fps.toFixed(1);
  refs.resolution.textContent = fmtResolution(state.stats);
  refs.format.textContent = state.stats.format;
  refs.frameId.textContent = state.stats.frameId.toString();
};

 const updateFrame = (uri: string) => {
  if (!refs.frame) return;
  refs.frame.src = uri;
  state.frameUri = uri;
  log(`Frame updated (${uri.length} bytes)`);
};

 const randomizeStats = () => {
  const fps = 12 + Math.random() * 8;
  const jitter = Math.random() > 0.5 ? 1 : -1;
  state.stats = {
    ...state.stats,
    fps,
    frameId: state.stats.frameId + 1,
    resolution: {
      width: 1280 + jitter * 16,
      height: 720 + jitter * 16,
    },
  };
  updateStatsPanel();
};

 const handleFile = (file: File) => {
  if (!file.type.startsWith("image")) {
    log("Rejected non-image payload");
    return;
  }
  const reader = new FileReader();
  reader.onload = (event) => {
    const result = event.target?.result;
    if (typeof result === "string") {
      updateFrame(result);
    }
  };
  reader.readAsDataURL(file);
  log(`Loaded frame from ${file.name}`);
};

 const wireEvents = () => {
  const applyBtn = document.getElementById("applyFrame");
  applyBtn?.addEventListener("click", () => {
    if (!refs.frameField) return;
    const payload = refs.frameField.value.trim();
    if (!payload) {
      log("Paste a data URI first");
      return;
    }
    if (!payload.startsWith("data:image")) {
      log("Payload must be a data:image URI");
      return;
    }
    updateFrame(payload);
  });

  const fileInput = document.getElementById("frameFile") as HTMLInputElement | null;
  fileInput?.addEventListener("change", (event) => {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      handleFile(file);
    }
  });

  const dropZone = document.getElementById("dropZone");
  if (dropZone) {
    ["dragover", "dragenter"].forEach((evt) => {
      dropZone.addEventListener(evt, (event) => {
        event.preventDefault();
        dropZone.classList.add("active");
      });
    });
    ["dragleave", "drop"].forEach((evt) => {
      dropZone.addEventListener(evt, (event) => {
        event.preventDefault();
        dropZone.classList.remove("active");
      });
    });
    dropZone.addEventListener("drop", (event) => {
      const file = event.dataTransfer?.files?.[0];
      if (file) {
        handleFile(file);
      }
    });
  }

  document.getElementById("toggleMode")?.addEventListener("click", () => {
    state.mode = state.mode === "edge" ? "raw" : "edge";
    refs.modeLabel!.textContent = state.mode === "edge" ? "Edge" : "Raw";
    log(`Mode switched to ${state.mode}`);
  });

  document.getElementById("simulateStream")?.addEventListener("click", () => {
    randomizeStats();
  });
};

 const init = () => {
  refs.frame = document.getElementById("framePreview") as HTMLImageElement;
  refs.fps = document.getElementById("fpsLabel")!;
  refs.resolution = document.getElementById("resolutionLabel")!;
  refs.format = document.getElementById("formatLabel")!;
  refs.frameField = document.getElementById("frameInput") as HTMLTextAreaElement;
  refs.log = document.getElementById("log")!;
  refs.frameId = document.getElementById("frameId")!;
  refs.modeLabel = document.getElementById("modeLabel")!;

  updateFrame(state.frameUri);
  updateStatsPanel();
  wireEvents();
  log("Viewer ready. Drop a PNG exported from the native pipeline.");
};

 document.addEventListener("DOMContentLoaded", init);
