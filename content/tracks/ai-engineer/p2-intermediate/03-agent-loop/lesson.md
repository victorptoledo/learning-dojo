# Agent Loop: Tools, Errors and Limits

Every agent framework hides the same loop: ask the model, run the tools it asks for, send the
results back, repeat. You write that loop yourself against a scripted fake model, with the parts
frameworks make you configure: argument checks, error results, parallel calls and the limits that
stop a runaway agent. Press **Enter** to create the kata folder and open it.
