# Terraform Gradle Plugin (TGP)

This plugin allows to fully delegate to Gradle all Terraform commands and also adds some new features!

## Index

- [Quick start guide](#quick-start-guide)
- [Why a Gradle plugin for Terraform?](#why-a-gradle-plugin-for-terraform)
- [Plugin overview](#plugin-overview)
    - [Lifecycle tasks](#lifecycle-tasks)
    - [Source sets](#source-sets)
        - [Create and manage new source sets](#create-and-manage-new-source-sets)
        - [Security check on tasks `apply` and `destroy`](#security-check-on-tasks-apply-and-destroy)
        - [Plan variables](#plan-variables)
        - [Output variables](#output-variables)
    - [Lock file](#lock-file)
    - [Local state file](#local-state-file)
    - [Resources references](#resources-references)
        - [`lambda` configuration](#lambda-configuration)
    - [`tfmodule` and libraries](#tfmodule-and-libraries)
        - [library metadata](#library-metadata)
        - [Consuming a library](#consuming-a-library)
        - [Publishing a library](#publishing-a-library)
    - [Distributions](#distributions)

## Quick start guide

Manage your Terraform sources with Gradle:

```kotlin
plugins {
    id("org.jetbrains.gradle.terraform") version "{latestVersion}"
}

terraform {

    version = "1.0.3"

    sourceSets {

        main {
            // apply and destroy are dangerous! put some checks on who can execute them!
            executeApplyOnlyIf { System.getenv("ENABLE_TF_APPLY") == "true" }
            executeDestroyOnlyIf { System.getenv("ENABLE_TF_DESTROY") == "true" }

            // variables in your sources without a default can be filled here
            planVariables = mapOf(
                "myVar" to System.getenv("MY_VAR"),
                "myString" to "trolololol"
            )
        }
    }
}
```

By default, sources are expected in `src/main/terraform`, you won't need to have any Terraform installed on your
machine, the plugin will take care of downloading and caching the executable; also the plugin will create the following
tasks:

- `terraformPlan`: generates the plan in `.bin` and `.json` format.
- `terraformApply`: applies the generated plan from `terraformPlan`.
- `terraformDestroyPlan`: generates the destroy plan in `.bin` and `.json` format.
- `terraformDestroyApply`: applies the plan from `terraformMainDestroyPlan`.

`terraform init` will be always executed before a plan automatically.

See an example [here](../examples/terraform/project-b/build.gradle.kts)

## Why a Gradle plugin for Terraform?

TL,DR:
Pure CLI tools makes things harder to reproduce on different machine because they require a setup and run many commands
by hand. Also declaring dependencies without a version is evil, you should not do it, even if there is a lock file. The
plugin addresses those issue gracefully in the Gradle way! Also, new swags are available such as Maven publications and
static resources reference.

#### Time and reproducibility

Terraform is a CLI tool, as such it requires someone (or some other tool) to control it in order to do things. The usual
way consists into running it by hand from a local machine and ensuring every time that you are using the right version
and running all the commands in the specific order. When developing and experiment, this approach not only is very
time-consuming, but also very much error-prone.

Delegating to Gradle both the setup of the environment and the execution of the steps greatly speeds ups the deployment
process, especially during development, and also makes sure that the build is reproducible in CI pipelines as well
without any addition Terraform specific setup.

#### Dependencies versioning hell

Unfortunately, often in our industry, we rely on implicit and/or partial versioning of dependencies in our project. This
means omitting the version by implicitly downloading locally what is the latest version of an imported library or
specifying something like `1.x`, which means `whatever higher version as long as it starts with "1."`. This implicit
versions will be resolved once during the initialization of the project (i.e. during `terraform init`, `npm install`
, `pip unfreze`, ecc...).

That approach, besides being very easily unreproducible, it exposes to dangers like unwanted upgrades to compromised
packages. A naive solution to address this issue could be generating a file during project initialization that "
remembers" which version you installed during the last `init`, adding it to your sources and reading the version you
want from there. This file is called `lock` file and should never be edited by hand.

Well congrats! Now you have a generated file bound to the version of the tool you are using, that is committed into the
repository, and it will probably be used by other developers with different setups. What could go wrong?

##### The Gradle way

Gradle takes a very different approach to versioning compared to Terraform, npm, pip and friends. You declare the
dependencies you want WITH ITS VERSION into your build script and Gradle will take care of downloading from the right
place the right dependency. You of course commit your build script because it contains all the build logic of your
project, making it 100% reproducible on every machine (assuming I wrote correctly the plugin 🙈).

Unfortunately, I cannot convert what is already out there into the Gradle format, as such this plugin also handles the
local lock file since providers are must always be published on the HashiCorp portal. To mitigate that, you should
ALWAYS set the version of a module or a provider in your sources, even if declaring the version of a dependency in the
actual sources is not really a good thing to do (but it's always better than relying on a generated file that does that
for you).

## Plugin overview

Once applied, the plugin consists almost entirely in the `terraform { }` extension for your `Project`. The receiver
class is [`TerraformExtension`](../src/main/kotlin/org/jetbrains/gradle/plugins/terraform/TerraformExtension.kt), from
there you can choose the version of terraform you want to use and the rest of the logic resides in the extension
`sourceSets`:

```kotlin
terraform { // TerraformExtension
    version = "1.0.6"
    showPlanOutputInConsole = true
    showInitOutputInConsole = true
    sourceSets { // NamedDomainObjectContainerScope<TerraformSourceSet>
        main { // TerraformSourceSet

        }
    }
}
```

### Lifecycle tasks

The plugin will create the following tasks which applies for all source sets:

- `terraformPlan`: generates the plan in `.bin` and `.json` format.
- `terraformApply`: applies the generated plan from `terraformPlan`.
- `terraformDestroyPlan`: generates the destroy plan in `.bin` and `.json` format.
- `terraformDestroyApply`: applies the plan from `terraformMainDestroyPlan`.

The plugin also creates tasks to handle each source set available in the project (
see [source sets section](#source-sets)); for example, the `main` source set will have `terraformMainPlan` to
run `terraform plan` only on said source set.

### Source sets

Each source set is backed by the
class [`TerraformSourceSet`](../src/main/kotlin/org/jetbrains/gradle/plugins/terraform/TerraformSourceSet.kt).

The `main` source set is always available and is set up to generate
a [`SoftwareComponenet`](https://docs.gradle.org/current/javadoc/org/gradle/api/component/SoftwareComponent.html)
called `"terraform"` retrievable with `componenets["terraform"]`, which will be used for the `maven-publish` plugin (
see [`tfmodule` section](#tfmodule-as-libraries)).

#### Create and manage new source sets

To create a new source set:

```kotlin
terraform {
    sourceSets {
        create("test") {
            // whatever
        }
    }
}
```

You can also create dependencies between source sets:

```kotlin
terraform {
    sourceSets {
        create("test") {
            dependsOn(main)
        }
    }
}
```

This means that the newly created `test` source set will have available all the sources of the `main` one, including the
resources. In case of a duplicate file name, an error will be thrown during task execution.

**IMPORTANT**:

- All source sets will have access to libraries added with `api` and `implementation`.
- only the `main` source set by default have access to the `lambda` resources (
  see [`lambda` configuration section](#lambda-configuration-section)). To make resources available to other sets
  use `TerraformSourceSet.addLambdasToResources`:

```kotlin
terraform {
    sourceSets {
        create("test") {
            addLambdasToResources = true
        }
    }
}
```

### Security check on tasks `apply` and `destroy`

The terraform commands `apply` and `destroy` can be very dangerous if executed accidentally. To reduce the probability
of such events, each source set has
two [`Spec<Boolean>`](https://docs.gradle.org/current/javadoc/org/gradle/api/specs/Spec.html) to check if the current
build is allowed to execute them:

```kotlin
terraform {
    sourceSets {
        main {
            executeApplyOnlyIf { System.getenv("ENABLE_TF_APPLY") == "true" }
            executeDestroyOnlyIf { System.getenv("ENABLE_TF_DESTROY") == "true" }
        }
    }
}
```

In this example you may want your machine to have some environment variables set in place to allow such important tasks
to be executed. By default, you are not allowed to execute those tasks.

### Plan variables

To inject a variable into the planning tasks you can use `TerraformSourceSet.planVariables`
or `TerraformSourceSet.planVariable()`:

```kotlin
terraform {
    sourceSets {
        main {
            planVariables = mapOf(
                "myVar" to System.getenv("MY_VAR"),
                "myVar2" to "43"
            )
            planVariable("my_other_var", "42")
        }
    }
}
```

**IMPORTANT**: `planVariables = mapOf(...)` will remove all previously set variables, while `planVariable(String, "42")`
will add a new one.

In the above example you have to know in advance the value of those variables. If you require to execute a task to know
the value of a variable, you can use `filePlanVariables: Map<String, File>` or `planVariable(String, File)`:

```kotlin
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformPlan

val outputFileVariable: File = file("$buildDir/out/file.txt")

val myGeneratorTask: TaskProvider<Task> by tasks.registering {
    outputs.file(outputFileVariable)
    doLast { outputFileVariable.writeText("hello world") }
}

// soon a mechanism to make this implicit will be created, 
// but we are not there yet!
tasks.withType<TerraformPlan> {
    dependsOn(myGeneratorTask)
}

terraform {
    sourceSets {
        main {
            planVariable("my_other_var", outputFileVariable)
        }
    }
}
```

In this example the content of `outputFileVariable` will be read during the execution of `terraform plan`
or `terraform destroy` and Gradle will make sure that task `myGeneratorTask` will always be executed before them.

### Output variables

Sometimes it is needed to read some value from the state file, whether it is local or remote. To do so:

```kotlin
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformOutput

val outputTask: TaskProvider<TerraformOutput> =
    terraform.sourceSets.main { output("myVar1", "myVar2") }
```

A task named `terraformMainOutputMyVar1MyVar2` will be created; it will output a JSON file containing the values of
those two variables according to [`terraform output` command](https://www.terraform.io/docs/cli/commands/output.html).

The [`TerraformOutput`](../src/main/kotlin/org/jetbrains/gradle/plugins/terraform/tasks/TerraformOutput.kt) task can be
tuned:

```kotlin
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformOutput

terraform {
    sourceSets {
        main {
            output("myVar1", "myVar2") {
                format = TerraformOutput.Format.RAW // default is TerraformOutput.Format.JSON
            }
        }
    }
}
```

## Lock file

The `.terraform.lock.hcl` is by default expected to be in `src/main/terraform/.terraform.lock.hcl` (if using a different
source set the path would of course change in `src/${sourceSetName}/terraform/.terraform.lock.hcl`). Since the plugin
copies somewhere else the sources in order to enhance them and add all dependencies, the lock file will be copied over
in the build directory and then copied back in the sources. The location of the lock file can be modified in the source
set:

```kotlin
terraform {
    sourceSets {
        main {
            lockFile = file("src/$name/terraform/lockfile.bin")
        }
    }
}
```

The plugin will handle name changes for you.

## Local state file

The `terraform.tfstate` is by default expected to be in `src/main/terraform/terraform.tfstate` (if using a different
source set the path would of course change in `src/${sourceSetName}/terraform/terraform.tfstate`). Since the plugin
copies somewhere else the sources in order to enhance them and add all dependencies, the state file will be copied over
in the build directory and then copied back in the sources. The location of the lock file can be modified in the source
set:

```kotlin
terraform {
    sourceSets {
        main {
            lockFile = file("src/$name/terraform/state.file")
        }
    }
}
```

The plugin will handle name changes for you.

In case the state file is remote, the plugin will ignore the local one.

## Resources references

The Terraform Gradle Plugin introduces the concept of resources just as well as the Java and Gradle plugins do.
A `resource` folder is expected for each source set and all the resources will be available in the sources
from `var.resources.`.

Let's see an example:

```text
|-src
|  |-main
|     |-terraform
|          |- main.tf
|     |-resources
|          |- myLambda.zip
```

Given the resource `myfile.txt`, in every `.tf` file inside `src/main/terraform` the variable `var.resources`
will be available with shape:

```terraform
variable "resources" {
  default = {
    myfile_txt = {
      path                   = "./resources/myLambda.txt"
      name                   = "myLambda.txt"
      name-without-extension = "myLambda"
      ext                    = "txt"
    }
  }
}
```

It can be used in `main.tf` easily:

```terraform
resource "aws_lambda_function" "server" {
  function_name = "my function"
  role          = "myRoleId"
  filename      = var.resources.myLambda.path   # MAGIC!
}
```

### `lambda` configuration

The Plugin also offers a convenient way to consume other project executables as lambdas.

Say that you have a Kotlin (or in general a JVM) project called `project-a`:

```kotlin
plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

tasks {
    shadowJar {
        archiveBaseName.set("projectA") // this will be the name of the jar
    }
}
// whatever you want to configure... 
```

Now from you Terraform project:

```kotlin
plugins {
    id("org.jetbrains.gradle.terraform") version "{latestVersion}"
}

dependencies {
    lambda(project(":project-a"))
}
```

Now from the `main` source sets you will be able to reference:

```terraform
resource "aws_lambda_function" "server" {
  function_name = "my_function"
  role          = "myRoleId"
  filename      = var.resources.projectA_jar.path // MAGIC!
}
```

Only the `main` source set by default have access to the `lambda` resources. To make resources available to other sets
use `TerraformSourceSet.addLambdasToResources`:

```kotlin
terraform {
    sourceSets {
        create("test") {
            addLambdasToResources = true
        }
    }
}
```

## `tfmodule` and libraries

The TGP introduces the concept of a Terraform Module file with extension `.tfmodule`. This file is a nonetheless
a `.zip` file containing the sources, resources and some metadata of a built module. The task `terraformMainModule`
generate such file for the `main` source set.

### Library metadata

Consists of a simple class:

```kotlin
data class TerraformModuleMetadata(var group: String, var moduleName: String, var version: String)
```

When a `tfmodule` is consumed, such metadata is used to allow other modules to consume the library.

It defaults with:

```kotlin
TerraformModuleMetadata(
    group = project.group.toString(),
    moduleName = project.group.toString(),
    version = project.version.toString()
)
```

and can be modified:

```kotlin
terraform {
    sourceSets {
        main {
            metadata { // TerraformModuleMetadata
                group = "whatever"
                moduleName = "you"
                version = "want"
            }
        }
    }
}
```

### Consuming a library

Once a library has been pushed into a Maven repository, it can be added to the project using either `implementation`
or `api` configurations:

```kotlin
dependencies {
    implementation("org.example:tf-example-module:1.0.0")
    api("org.example:tf-example-module2:1.0.0")
}
```

Now, assuming that the library [metadata](#library-metadata) is the same of the Maven Coordinates, in your `.tf` files
you will be able to:

```terraform
module "myModule" {
  //noinspection TFNoInterpolationsAllowed # Needed for the IJ TF plugin
  source = modules.org.example.tf-example-module # MAGIC!

  # configure the rest normally...
}
```

Note that you can also consume local modules that uses the TGP like:

```kotlin
dependencies {
    implementation(project(":terraform-project"))
}
```

### Publishing a library

To publish a library you need the `maven-publish` plugin in your project:

```kotlin
plugins {
    id("org.jetbrains.gradle.terraform") version "{latestVersion}"
    `maven-publish`
}
```

Now a Maven publication called `terraform` is added to `publishing.publications`. You can use
the `maven-publish` `publish` tasks to push your library wherever you want! If you want to publish it on Maven Central,
I would recommend having a look at the [`Gradle Nexus Publish Plugin`](https://github.com/gradle-nexus/publish-plugin).

Note that only the dependencies declared into the `api` configuration will be declared in the publication, just like the
Java plugin does.

## Distributions

The TGP does many transformations on the sources before executing `terraform` on them. If you want to have a
distribution of those modified sources for whatever reason, just apply the `distribution` plugin:

```kotlin
plugins {
    id("org.jetbrains.gradle.terraform") version "{latestVersion}"
    distribution
}
```

Now the task `distZip` and `distTar` will generate a distribution with all the generated sources and resources with the
Terraform executable used by the build in the current machine.

