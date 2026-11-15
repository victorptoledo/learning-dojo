---
from: csharp
status: draft
concept: "Adapter, decorator, proxy and facade"
from_way: "Inheriting from a vendor client to override one method, or wrapping it in every service."
native_way: "Adapter classes behind your own interface; decorators registered in DI (with Scrutor's `Decorate` or by hand); `DispatchProxy` or Castle proxies; `HttpClient` as a facade."
sensei_tip: "`DelegatingHandler` in HttpClient is a decorator chain: each handler wraps the next, which is how Polly adds retries and timeouts to outgoing calls."
---

| C# | Pattern |
| --- | --- |
| a class implementing `IPaymentGateway` over a vendor SDK | adapter |
| `DelegatingHandler`, `BufferedStream` | decorator |
| `System.Reflection.DispatchProxy` | proxy |
| `ReadOnlyCollection<T>` | protection proxy |
| `HttpClient` | facade |
