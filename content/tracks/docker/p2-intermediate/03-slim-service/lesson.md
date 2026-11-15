# Slim Service: a Multi-stage Build

The single-stage image from Checkpoint 1 works, but it ships a whole Go toolchain and the
source code, and it runs as root. Rewrite it as a multi-stage build. Press **Enter** to
create the kata folder; the starter Dockerfile is the working single-stage version.
