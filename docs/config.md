Configuration
=============

This module can be configured by editing the configuration file. This file is installed in `/etc/opt/dans.knaw.nl/dd-vault-metadata/config.yml` when using the RPM.
The settings are explained with comments in the file itself. An on-line version of the latest configuration file can be found
[here](https://github.com/DANS-KNAW/dd-vault-metadata/blob/master/src/main/assembly/dist/cfg/config.yml){:target=_blank}.

Dataverse configuration
-----------------------
To configure Dataverse to call this service, you must add it as an [http/authext](https://guides.dataverse.org/en/latest/developers/workflows.html#http-authext){:target=_blank}
workflow step to the default `PrePublishDataset` workflow. The JSON to use in the step is included in `INSTALL_DIR/install/workflow-step.json`.
