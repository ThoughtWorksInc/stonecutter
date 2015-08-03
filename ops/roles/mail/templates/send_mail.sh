#!/bin/bash

PROVIDER_DIR=./providers
VERBOSE=0
GREEN='\033[0;36m'
NC='\033[0m' # No Color

if [[ ! -d $PROVIDER_DIR ]];
then
  echo "Provider directory $PROVIDER_DIR does not exist"
  exit 1
fi

SUPPORTED_EMAIL_PROVS=($( ls -A $PROVIDER_DIR ))

if [[ ${{ '{#' }}SUPPORTED_EMAIL_PROVS[@]} -eq 0 ]]
then
  echo "Provider directory $PROVIDER_DIR is empty"
  echo 'Please provide a provider'
  exit 1
fi

provider=$EMAIL_SERVICE_PROVIDER

log () {
  if [[ $VERBOSE -eq 1 ]]; then
      printf "${GREEN}$@${NC}"
      echo ""
  fi
}

usage() {
  cat <<EOF
    Usage: $0 [options] to subject body
    -h    Display help message
    -p    Service Provider (supported: ${SUPPORTED_EMAIL_PROVS[@]} - Current: $provider)
    -v    Verbose mode
    -f    From email address - Default value from FROM envvar ($FROM)
EOF
exit 1;
}

while getopts "hvf:p:" opt; do
  case $opt in
    h)
      usage
      exit 0
      ;;
    v)
      VERBOSE=1
      ;;
    f)
      FROM=$OPTARG
      ;;
    p)
      provider=$OPTARG
      ;;
  esac
done
# Remove options from argument list
shift $((OPTIND-1))

# Final checks

# Require 3 input arguments
if [[ $# -lt 3 ]]
then
  usage
fi

# Driver must exist for selected 'provider'
provider_driver_path=$PROVIDER_DIR/$provider

if [[ ! -x $provider_driver_path ]]
then
  echo "Provider driver $provider_driver_path does not exist, or is not executable"
  exit 1
fi

log "Calling provider $provider at $provider_driver_path"
log "arguments: $@"
log "from: $FROM"
$provider_driver_path "$@"
