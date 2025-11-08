# NTexUp Quick Start Guide
This guide walks you through installing Nuts, setting up NTexUp, and creating your first document project.
NTexUp is a flexible, template-driven document generator built on top of the Nuts package manager.

## Install Nuts
First, install the latest version of Nuts:
```bash
curl -s https://thevpc.net/nuts/install-latest.sh | bash
```

## Restart your terminal
After installation, restart your terminal to apply environment changes:
```bash
exit
```
Then reopen a new terminal session.

## Install NTexUp
Once Nuts is installed, use it to install NTexUp:
```bash
nuts install ntexup
```
This command downloads and installs the latest stable version of NTexUp and its dependencies.


## Create a document project
Create a new workspace for your first project:

```bash
cd ~
mkdir my-first-document
cd my-first-document
nuts ntexup new -t=classic
```

When executed, NTexUp initializes a new document project using the selected template.
You’ll see a message similar to the following:

```result
2025-10-14 21:37:47.349 WARNING ALERT   : reset workspace
     __        __       
  /\ \ \ _  __/ /______   Network Updatable Things Services
 /  \/ / / / / __/ ___/   The Free and Open Source Package Manager
/ /\  / /_/ / /_(__  )    for Java and other Things ... by thevpc
\_\ \/\__,_/\__/____/     https://github.com/thevpc/nuts
    version 0.8.8.0
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
[2025-10-14T20:37:52.130581188Z] [WARNING] [] took 2s 989ms 913ns to clone repo git@github.com:thevpc/ntexup-templates.git to /home/vpc/.nuts/ws/default-workspace/cache/id/net/thevpc/ntexup/ntexup/0.8.8.0/ntexup/github/thevpc/ntexup-templates
took 2s 989ms 913ns to clone repo git@github.com:thevpc/ntexup-templates.git to /home/vpc/.nuts/ws/default-workspace/cache/id/net/thevpc/ntexup/ntexup/0.8.8.0/ntexup/github/thevpc/ntexup-templates
```

## View the Generated Project Structure

You can now inspect the generated project files:

```bash
tree
```

```
.
├── 01-styles
│   └── 001-styles.ntx
├── 02-pages
│   ├── 0001-intro
│   │   ├── 0001-cover.ntx
│   │   └── 0010-plan.ntx
│   └── 9999-conclusion
│       ├── 9901-conclusion.ntx
│       └── 9999-thankyou.ntx
└── main.ntx

5 directories, 6 files
```
Each .ntx file defines a part of your document (such as sections, styles, or layouts).
The main.ntx file acts as the project’s entry point.

## Select a Template
NTexUp supports multiple document templates.
To view all available templates, run:

```bash
nuts ntexup list-templates
```

You’ll see a list similar to this:
```result
[2025-10-14T21:22:42.997Z] [FINEST] [] repository template not found 'local' at '/home/vpc/.nuts/ws/default-workspace/conf/id/net/thevpc/ntexup/ntexup/SHARED/templates'
[2025-10-14T21:22:43.001Z] [FINEST] [] repository template not found 'user' at '/home/vpc/.config/nuts/ntexup/templates'
[2025-10-14T21:22:43.001Z] [FINEST] [] repository template not found 'system' at '/etc/opt/nuts/conf/ntexup/templates'
Already up to date.
[2025-10-14T21:22:45.597Z] [WARNING] [] took 2s 594ms 719us 267ns to pull repo git@github.com:thevpc/ntexup-templates.git to /home/vpc/.nuts/ws/default-workspace/cache/id/net/thevpc/ntexup/ntexup/0.8.8.0/ntexup/github/thevpc/ntexup-templates
took 2s 594ms 719us 267ns to pull repo git@github.com:thevpc/ntexup-templates.git to /home/vpc/.nuts/ws/default-workspace/cache/id/net/thevpc/ntexup/ntexup/0.8.8.0/ntexup/github/thevpc/ntexup-templates
central-github:ibtihel-small#1.0 ibtihel         github://thevpc/ntexup-templates/ibtihel/v1.0/templates/small
central-github:ibtihel-medium#1.0 ibtihel  (*)    github://thevpc/ntexup-templates/ibtihel/v1.0/templates/medium
central-github:ibtihel-large#1.0 ibtihel         github://thevpc/ntexup-templates/ibtihel/v1.0/templates/large
central-github:classic-small#1.0 classic         github://thevpc/ntexup-templates/classic/v1.0/templates/small
central-github:classic-medium#1.0 classic  (*)    github://thevpc/ntexup-templates/classic/v1.0/templates/medium
central-github:classic-large#1.0 classic         github://thevpc/ntexup-templates/classic/v1.0/templates/large
central-github:eniso-small#1.0 eniso         github://thevpc/ntexup-templates/eniso/v1.0/templates/small
central-github:eniso-medium#1.0 eniso  (*)    github://thevpc/ntexup-templates/eniso/v1.0/templates/medium
central-github:eniso-large#1.0 eniso         github://thevpc/ntexup-templates/eniso/v1.0/templates/large
```

You can specify a different template when creating a project, for example:
```
nuts ntexup new --template=classic
```

## Run the NTexUp Viewer
To preview your project and view changes in real-time, use the viewer command:
```bash
nuts ntexup show .
```
This launches a local viewer that automatically refreshes as you modify your .ntx files.


## Edit and Customize

Open the generated .ntx files in your favorite text editor and make modifications.
NTexUp automatically detects changes and updates the rendered output in the viewer.
You can adjust styles, layouts, and page content to fully customize your document.

## Advanced command

```bash
nuts ntexup new -t=classic --show --show-doc
```

## Viewing Documentation

NTexUp includes built-in command-line documentation and examples.

To explore the documentation interactively, run:
```bash
nuts ntexup view-doc
```

You can also browse official .ntx documentation files at:

🔗 https://github.com/thevpc/ntexup-doc-slides

These examples demonstrate NTexUp’s syntax, presentation structure, and integration capabilities.

## Running in a containerized environment (Docker)

NTexUp (and any Nuts-based application) can run directly inside a Docker container or cloud IDEs like Gitpod without needing to build a custom Docker image.
Note that only pdf generation would work because ntexup viewer need display manager to work.
besides, you container should include git (for now git is not bundled with texup) if you want to use git hosted templates. 

### Step 1: Launch a Docker container with Java

On your terminal, run:
```bash
docker run -it --rm openjdk:8 bash -c "$(curl -sSL https://thevpc.net/nuts/bootstrap-container-latest.sh)"
```

Notes:
* This command pulls the OpenJDK 8 image (you can choose another version if needed) and bootstraps the container with Nuts.
* The script will create a non-root user (nuts by default), detect Java, and install the latest version of Nuts inside the container.
* After the script finishes, you’ll drop into an interactive bash shell inside the container, ready to run Nuts commands.

### Step 2: Run NTexUp

Once inside the container shell, you can launch NTexUp (or any Nuts app) with:
```bash
nuts -y ntexup <your-arguments>
```
The -y option automatically confirms any prompts.

Any additional arguments you pass will be forwarded directly to NTexUp.

#### Optional: Enable technical/debug messages
To see detailed technical information during container bootstrap, you can set the TECH_ECHO environment variable when starting the container:

```bash
docker run -it --rm -e NUTS_CONTAINER_VERBOSE=1 openjdk:8 bash -c "$(curl -sSL https://thevpc.net/nuts/bootstrap-container-latest.sh)"
```

## Conclusion
You’ve now installed Nuts, set up NTexUp, and created your first document project.
Explore available templates, experiment with layouts, and build professional documents effortlessly.

For more details, visit the official NTexUp repository:
👉 https://github.com/thevpc/ntexup-templates

