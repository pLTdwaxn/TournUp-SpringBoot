vault {
    address = "https://openbao-preview:8200"
    ca_cert = "/run/openbao/trust/ca.crt"
}

auto_auth {
    method "approle" {
        mount_path = "auth/approle"

        config = {
            role_id_file_path               = "/run/openbao/bootstrap/role-id"
            secret_id_file_path             = "/run/openbao/bootstrap/secret-id"
            remove_secret_id_file_after_reading = false
        }
    }

    sink "file" {
        config = {
            path = "/run/openbao/token/token"
            mode = 0640
            uid  = -1
            gid  = -1
        }
    }
}
