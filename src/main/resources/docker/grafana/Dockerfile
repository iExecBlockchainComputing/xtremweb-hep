FROM grafana/grafana:5.0.4

COPY ./init.sh /init.sh
COPY ./dashboards dashboards

ENTRYPOINT ["/usr/bin/env"]

RUN chmod +x /init.sh

CMD ["bash", "/init.sh"]
