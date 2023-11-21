# ansible-jenkins-shared-utility-lib

This repo is a simple library that contains some functions to integrate with AWX - upstream project of Ansible Automation Platform ex Ansible Tower.



## closure example

```groovy
def (organizationListCode, organizationID) = tower.performGetResourceID({
        ->
            return aapcli.getOrganizationList()
        },
        env.TOWER_ORGANIZATION_ID,
        " Organization + ${env.TOWER_ORGANIZATION_ID} not found "
    )
```