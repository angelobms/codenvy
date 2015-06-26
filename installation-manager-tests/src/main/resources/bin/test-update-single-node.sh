#!/bin/bash
#
# CODENVY CONFIDENTIAL
# ________________
#
# [2012] - [2015] Codenvy, S.A.
# All Rights Reserved.
# NOTICE: All information contained herein is, and remains
# the property of Codenvy S.A. and its suppliers,
# if any. The intellectual and technical concepts contained
# herein are proprietary to Codenvy S.A.
# and its suppliers and may be covered by U.S. and Foreign Patents,
# patents in process, and are protected by trade secret or copyright law.
# Dissemination of this information or reproduction of this material
# is strictly forbidden unless prior written permission is obtained
# from Codenvy S.A..
#

. ./lib.sh

vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

printAndLog "TEST CASE: Update single-node Codenvy"

log "Available versions: "${AVAILABLE_CODENVY_VERSIONS}
log "Previos versions: "${PREV_CODENVY_VERSION}
log "Latest versions: "${LATEST_CODENVY_VERSION}

installCodenvy ${PREV_CODENVY_VERSION}
auth "admin" "password"
validateInstalledVersion ${PREV_CODENVY_VERSION}

executeIMCommand "im-download" "codenvy" "${LATEST_CODENVY_VERSION}"
executeIMCommand "im-install" "codenvy" "${LATEST_CODENVY_VERSION}"
auth "admin" "password"
validateInstalledVersion ${LATEST_CODENVY_VERSION}

printAndLog "RESULT: PASSED"
retrieveInstallLog

vagrantDestroy
