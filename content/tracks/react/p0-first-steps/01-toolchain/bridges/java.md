---
from: java
status: draft
concept: "A view layer, not a whole platform"
from_way: "Spring Boot or Jakarta EE give you a full platform; a desktop UI in Swing or JavaFX mutates widget objects (label.setText) when data changes."
native_way: "React is only the view: a component returns the UI for its state, and React updates the DOM. Routing, data and builds come from libraries or a framework."
sensei_tip: "Stop thinking in widgets you mutate. In React you never call setText: you change state, and the component's return value is the new screen."
---

| Java | React |
| --- | --- |
| Maven/Gradle project, `mvn spring-boot:run` | `npm create vite@latest`, `npm run dev` |
| `public static void main` | `src/main.tsx` with `createRoot(...).render(<App />)` |
| `label.setText("Saved")` | `setStatus("saved")`, and JSX shows `{status}` |
| JavaFX scene graph | The component tree (seen in React DevTools) |
| Spring Boot starters bundle features | Frameworks (Next.js, React Router) bundle routing and data |
| Assertions (`-ea`) only in development | StrictMode checks only in development |
