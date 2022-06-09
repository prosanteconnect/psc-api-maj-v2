project = "psc-api-maj-v2"

# Labels can be specified for organizational purposes.
labels = {
  "domaine" = "psc"
}

runner {
  enabled = true
  data_source "git" {
    url = "https://github.com/prosanteconnect/psc-api-maj-v2.git"
    ref = var.datacenter
  }
  poll {
    enabled = true
  }
}

# An application to deploy.
app "prosanteconnect/psc-api-maj-v2" {
  # Build specifies how an application should be deployed. In this case,
  # we'll build using a Dockerfile and keeping it in a local registry.
  build {
    use "docker" {
      build_args = {
        "proxy_address" = var.proxy_address
      }
      dockerfile = "${path.app}/${var.dockerfile_path}"
    }
    # Uncomment below to use a remote docker registry to push your built images.
    registry {
      use "docker" {
        image = "${var.registry_path}/psc-api-maj"
        tag = gitrefpretty()
        encoded_auth = filebase64("/secrets/dockerAuth.json")
      }
    }
  }

  # Deploy to Nomad
  deploy {
    use "nomad-jobspec" {
      jobspec = templatefile("${path.app}/psc-api-maj.nomad.tpl", {
        datacenter = var.datacenter
      })
    }
  }
}

variable "datacenter" {
  type = string
  default = "dc1"
}

variable "registry_username" {
  type = string
  default = ""
}

variable "registry_password" {
  type = string
  default = ""
}

variable "proxy_address" {
  type = string
  default = "proxy_address"
}

variable "dockerfile_path" {
  type = string
  default = "Dockerfile"
}

variable "registry_path" {
  type = string
  default = "registry.repo.proxy-dev-forge.asip.hst.fluxus.net/prosanteconnect"
}
