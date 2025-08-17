FROM ghcr.io/ombra-chat/builder:v0.0.2 AS builder-stage

# TODO: move this to builder
RUN apt install -y xdg-utils

RUN git clone https://github.com/ombra-chat/tdlib-rs.git
RUN git clone https://github.com/ombra-chat/ombra-chat.git

WORKDIR /ombra-chat

ARG TAG=master
RUN git checkout $TAG

RUN ldconfig
RUN bash -c "source $HOME/.nvm/nvm.sh && source $HOME/.cargo/env && npm install && npm run tauri build"

FROM scratch
COPY --from=builder-stage /ombra-chat/src-tauri/target/release/bundle/deb/ombra-chat*.deb /
COPY --from=builder-stage /ombra-chat/src-tauri/target/release/bundle/rpm/ombra-chat*.rpm /
COPY --from=builder-stage /ombra-chat/src-tauri/target/release/bundle/appimage/ombra-chat*.AppImage /
