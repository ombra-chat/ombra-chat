FROM ghcr.io/ombra-chat/builder:v0.0.3 AS builder-stage

RUN git clone https://github.com/ombra-chat/tdlib-rs.git
RUN git clone https://github.com/ombra-chat/ombra-chat.git

WORKDIR /ombra-chat

ARG TAG=master
RUN git checkout $TAG

# See https://github.com/linuxdeploy/linuxdeploy/issues/154
ENV APPIMAGE_EXTRACT_AND_RUN=1

RUN bash -c "source $HOME/.nvm/nvm.sh && source $HOME/.cargo/env && npm install && npm run tauri build"

FROM scratch
COPY --from=builder-stage /ombra-chat/src-tauri/target/release/bundle/deb/ombra-chat*.deb /
COPY --from=builder-stage /ombra-chat/src-tauri/target/release/bundle/rpm/ombra-chat*.rpm /
COPY --from=builder-stage /ombra-chat/src-tauri/target/release/bundle/appimage/ombra-chat*.AppImage /
