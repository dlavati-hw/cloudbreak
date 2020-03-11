base:
           '*':
             - nginx
             - fluent
             - freeipa
             - freeipa.services
             - dns

           'roles:freeipa_primary':
             - match: grain
             - freeipa.primary-install
             - freeipa.common-install

           'roles:freeipa_replica':
             - match: grain
             - freeipa.replica-install
             - freeipa.common-install