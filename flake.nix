{
  inputs = {
    nixpkgs = {
      url = "nixpkgs/nixos-24.05";
    };
    gradle2nix = {
      url = "github:tadfisher/gradle2nix/v2";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, gradle2nix }: let
    system = "x86_64-linux";
    pkgs = import nixpkgs {inherit system;};
  in rec {
    packages.${system} = rec {
      source = gradle2nix.builders.x86_64-linux.buildGradlePackage rec {
        name = "rp-utils";
        version = "1.0";
        pname = "wordcount";
        lockFile = ./gradle.lock;
        src = ./.;
        gradleBuildFlags = ["build -x test"];
        gradleInstallFlags = ["installDist -x test"];

        installPhase = ''
          mkdir -p $out/{lib,share}/${name}
          cp ./app/build/libs/app-all.jar $out/lib/${name}/rp-utils.jar
          cp -r $src/* $out/share/${name}
        '';
      };
      default = pkgs.writeScriptBin "rp-utils.sh" ''
        #!${pkgs.bash}/bin/bash
        ${pkgs.jdk21}/bin/java -jar ${source}/lib/rp-utils/rp-utils.jar $@
      '';
      vm = (nixpkgs.lib.nixosSystem {
        inherit system;
        modules = [
          nixosModules.default
          {
            system.stateVersion = "24.05";
            services.rp-utils.enable = true;
            users.users.root.password = "1234";
            virtualisation.vmVariant.virtualisation.graphics = false;
          }
        ];
      }).config.system.build.vm;
    };

    nixosModules.default = {pkgs, lib, config, ...}: {
      options.services.rp-utils = with lib.types; {
        enable = lib.mkOption {
          type = bool;
          default = false;
        };
        runtimeDir = lib.mkOption {
          type = str;
          default = "/var/run/rp-utils";
        };
      };
      config = {
        systemd.services.rp-utils = {
          enable = config.services.rp-utils.enable;
          path = with pkgs; [
            texliveFull
            bash
            sqlite
            python311
          ];
          serviceConfig = {
            WorkingDirectory = config.services.rp-utils.runtimeDir;
            ExecStart = "${pkgs.jdk21}/bin/java -jar ${packages.${system}.source}/lib/rp-utils/rp-utils.jar";
          };
          wantedBy = ["default.target"];
          after = ["network-online.target"];
          wants = ["network-online.target"];
        };
      };
    };
  };
}
