job "psc-api-maj-v2" {
  datacenters = [
    "${datacenter}"]
  type = "service"
  vault {
    policies = [
      "psc-ecosystem"]
    change_mode = "restart"
  }

  affinity {
    attribute = "$\u007Bnode.class\u007D"
    value = "standard"
  }

  group "psc-api-maj-v2" {
    count = "1"
    restart {
      attempts = 3
      delay = "60s"
      interval = "1h"
      mode = "fail"
    }

    update {
      max_parallel      = 1
      canary            = 1
      min_healthy_time  = "30s"
      progress_deadline = "5m"
      healthy_deadline  = "2m"
      auto_revert       = true
      auto_promote      = false
    }

    network {
      port "http" {
        to = 8080
      }
    }

    scaling {
      enabled = true
      min = 1
      max = 5

      policy {
        cooldown = "180s"
        check "few_requests" {
          source = "prometheus"
          query = "min(max(http_server_requests_seconds_max{_app='psc-api-maj-v2'}!= 0)by(instance))"
          strategy "threshold" {
            upper_bound = 2
            delta = -1
          }
        }

        check "many_requests" {
          source = "prometheus"
          query = "min(max(http_server_requests_seconds_max{_app='psc-api-maj-v2'}!= 0)by(instance))"
          strategy "threshold" {
            lower_bound = 0.5
            delta = 1
          }
        }
      }
    }

    task "psc-api-maj-v2" {
      driver = "docker"
      config {
        image = "${artifact.image}:${artifact.tag}"
        ports = [
          "http"]
      }

      template {
        destination = "local/file.env"
        env = true
        data = <<EOH
JAVA_TOOL_OPTIONS="-Xms256m -Xmx1g -XX:+UseG1GC -Dspring.config.location=/secrets/application.properties"
EOH
      }

      template {
        data = <<EOF
spring.application.name=psc-api-maj
server.servlet.context-path=/psc-api-maj
logging.level.org.springframework.data.mongodb.core.MongoTemplate=INFO
server.error.include-stacktrace=never
spring.data.mongodb.host={{ range service "psc-mongodb" }}{{ .Address }}{{ end }}
spring.data.mongodb.port={{ range service "psc-mongodb" }}{{ .Port }}{{ end }}
spring.data.mongodb.database=mongodb
{{ with secret "psc-ecosystem/mongodb" }}spring.data.mongodb.username={{ .Data.data.root_user }}
spring.data.mongodb.password={{ .Data.data.root_pass }}{{ end }}
spring.data.mongodb.auto-index-creation=false
EOF
        destination = "secrets/application.properties"
        change_mode = "restart"
      }

      resources {
        cpu = 500
        memory = 1280
      }


      service {
        name = "$\u007BNOMAD_JOB_NAME\u007D"
        tags = ["urlprefix-/psc-api-maj"]
        canary_tags = ["canary instance to promote"]
        port = "http"
        check {
          type = "tcp"
          port = "http"
          interval = "30s"
          timeout = "2s"
          failures_before_critical = 5
        }
      }

      service {
        name = "metrics-exporter"
        port = "http"
        tags = [
          "_endpoint=/psc-api-maj/v2/actuator/prometheus",
          "_app=psc-api-maj-v2",]
      }
    }

    task "log-shipper" {
      driver = "docker"
      restart {
        interval = "30m"
        attempts = 5
        delay = "15s"
        mode = "delay"
      }
      meta {
        INSTANCE = "$\u007BNOMAD_ALLOC_NAME\u007D"
      }
      template {
        data = <<EOH
LOGSTASH_HOST = {{ range service "logstash" }}{{ .Address }}:{{ .Port }}{{ end }}
ENVIRONMENT = "${datacenter}"
EOH
        destination = "local/file.env"
        env = true
      }
      config {
        image = "prosanteconnect/filebeat:7.14.2"
      }
    }
  }
}
