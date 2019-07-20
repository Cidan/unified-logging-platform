FROM alpine:3.8
COPY ./entrypoint.sh /
RUN apk add --no-cache bc && chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
CMD [ "10", "40" ]