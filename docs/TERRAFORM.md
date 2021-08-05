## Terraform plugin

Manage your Terraform sources with Gradle:

```kotlin
plugins {
    id("org.jetbrains.gradle.terraform") version "{latestVersion}"
}

dependencies {
    // "my-other-project" must have shadow plugin applied
    lambda(project(":my-other-project")) 
}

terraform {
    
    version = "1.0.3"
    
    sourceSets {
        executeApplyOnlyIf { System.getenv("ENABLE_TF_APPLY") == "true" }
        executeDestroyOnlyIf { System.getenv("ENABLE_TF_DESTROY") == "true" }

        main {
            planVariables = mapOf(
                "myVar" to System.getenv("MY_VAR"),
                "lambdaJarsDirectory" to lambdasDirectory.absolutePath
            )
        }
    }
}

```

By default, sources are expected in `src/main/terraform`, also the plugin w create many tasks:
- `terraformMainPlan`: generates the plan in `.bin` and `.json` format.
- `terraformMainApply`: applies the plan from `terraformMainPlan`.
- `terraformMainDestroyPlan`: generates the destroy plan in `.bin` and `.json` format.
- `terraformMainDestroyApply`: applies the plan from `terraformMainDestroyPlan`.

`terraform init` will be always executed before a plan automatically.

See an example [here](../examples/terraform/build.gradle.kts)
