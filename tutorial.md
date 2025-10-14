
## Install nuts
```bash
curl -s https://thevpc.net/nuts/install-latest.sh | bash
```

## restart you terminal
```bash
exit
```

## install ntexup
```bash
    nuts -ZyS install ntexup
```

## create a document project
```bash
    cd ~
    mkdir my-first-document
    cd my-first-document
    nuts ntexup new --template=central-github:classic-large
```

you will see something like

```
2025-10-14 21:37:47.349 WARNING ALERT   : reset workspace
     __        __       
  /\ \ \ _  __/ /______   Network Updatable Things Services
 /  \/ / / / / __/ ___/   The Free and Open Source Package Manager
/ /\  / /_/ / /_(__  )    for Java and other Things ... by thevpc
\_\ \/\__,_/\__/____/     https://github.com/thevpc/nuts
    version 0.8.7.0
location:/home/vpc/.nuts/ws/default-workspace  (PeculiarRed)
╭────────────────────────────────────────────────────────────╮
│ This is the first time nuts is launched for this workspace │
╰────────────────────────────────────────────────────────────╯

force updating scripts to point to current workspace : 
	.bashrc, .cshrc, .kshrc, .nuts-env.fish, .nuts-env.sh
	.nuts-init.fish, .nuts-init.sh, .nuts-term-init.fish, .nuts-term-init.sh, .profile
	.zshenv, config.fish, net.thevpc.nuts-nuts.desktop, net.thevpc.nuts-nuts.menu, nuts
	nuts-term
[2025-10-14T20:37:49.139669972Z] [WARNING] [] repository template not found 'local' at '/home/vpc/.nuts/ws/default-workspace/conf/id/net/thevpc/ntexup/ntexup/SHARED/templates'
[2025-10-14T20:37:49.140385388Z] [WARNING] [] repository template not found 'user' at '/home/vpc/.config/nuts/ntexup/templates'
[2025-10-14T20:37:49.140776818Z] [WARNING] [] repository template not found 'system' at '/etc/opt/nuts/conf/ntexup/templates'
Cloning into 'ntexup-templates'...
[2025-10-14T20:37:52.130581188Z] [WARNING] [] took 2s 989ms 913ns to clone repo git@github.com:thevpc/ntexup-templates.git to /home/vpc/.nuts/ws/default-workspace/cache/id/net/thevpc/ntexup/ntexup/0.8.7.0/ntexup/github/thevpc/ntexup-templates
took 2s 989ms 913ns to clone repo git@github.com:thevpc/ntexup-templates.git to /home/vpc/.nuts/ws/default-workspace/cache/id/net/thevpc/ntexup/ntexup/0.8.7.0/ntexup/github/thevpc/ntexup-templates
Enter template url. You can choose from the following :
[#1 ] central-github:ibtihel-small#1.0 : github://thevpc/ntexup-templates/ibtihel/v1.0/templates/small
[#2 ] central-github:ibtihel-medium#1.0 : github://thevpc/ntexup-templates/ibtihel/v1.0/templates/medium
[#3 ] central-github:ibtihel-large#1.0 : github://thevpc/ntexup-templates/ibtihel/v1.0/templates/large
[#4 ] central-github:classic-small#1.0 : github://thevpc/ntexup-templates/classic/v1.0/templates/small
[#5 ] central-github:classic-medium#1.0 : github://thevpc/ntexup-templates/classic/v1.0/templates/medium
[#6 ] central-github:classic-large#1.0 : github://thevpc/ntexup-templates/classic/v1.0/templates/large
[#7 ] central-github:eniso-small#1.0 : github://thevpc/ntexup-templates/eniso/v1.0/templates/small
[#8 ] central-github:eniso-medium#1.0 : github://thevpc/ntexup-templates/eniso/v1.0/templates/medium
[#9 ] central-github:eniso-large#1.0 : github://thevpc/ntexup-templates/eniso/v1.0/templates/large
 ? : 5
```

when you type '5' you will choose 'classic-medium-central-github' template

## run ntexup viewer

