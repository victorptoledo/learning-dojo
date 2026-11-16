---
from: csharp
status: draft
concept: "One immutable artefact for every environment"
from_way: "A web deploy package installed on IIS servers that have their own .NET runtime version."
native_way: "The image is built FROM the .NET SDK and runs FROM the ASP.NET runtime image; the runtime travels with the app."
sensei_tip: "`dotnet publish` can build an image without a Dockerfile (PublishContainer); chiseled Ubuntu images are the .NET equivalent of distroless."
---

| C# world | Containers |
| --- | --- |
| the server's .NET runtime | the runtime base image |
| `appsettings.Production.json` | environment variables or mounted config |
| a web deploy package | an image |
| NuGet feeds | an image registry |
