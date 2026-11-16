# Stream Assembler: Server-Sent Events to a Response

SDKs hide it, but every streamed model answer is a long HTTP response of server-sent events. You
write the code that turns that stream into a complete response: text, tool calls with arguments
that arrive in fragments, the stop reason and usage, while showing text to the user as it comes.
Press **Enter** to create the kata folder and open it.
