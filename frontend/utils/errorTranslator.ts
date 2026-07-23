const ERROR_CODE_TO_I18N_KEY: Record<string, string> = {
  DOMAIN_MISSING_FIELD_MAPPING: 'error_domain_missing_id',
  DATA_QUALITY_CHECK_FAILED: 'error_dq_failed',
  DEDUPLICATION_FAILED: 'error_dedup_failed',
  UPDATE_PENDING_CREATION: 'error_update_pending_creation',
  UPDATE_PENDING_UPDATE: 'error_update_pending_update',
  DELETE_PENDING_CREATION: 'error_delete_pending_creation',
  NOT_STEP_ASSIGNEE: 'error_not_assignee',
  STEP_NOT_PENDING: 'error_step_not_pending',
  USERNAME_ALREADY_EXISTS: 'error_username_exists',
  INVALID_CREDENTIALS: 'error_invalid_credentials',
  NODE_DOMAIN_MISMATCH: 'error_node_domain_mismatch',
  FIELD_NODE_MISMATCH: 'error_field_node_mismatch',
  FIELD_DOMAIN_MISMATCH: 'error_field_domain_mismatch',
  SECTOR_DOMAIN_MISMATCH: 'error_sector_domain_mismatch',
  UPLOAD_DIR_FAIL: 'error_upload_dir_fail',
  UPLOAD_FILE_FAIL: 'error_upload_file_fail',
  RESOURCE_NOT_FOUND: 'error_resource_not_found',
  ACCESS_DENIED: 'error_access_denied',
  INVALID_INPUT: 'error_invalid_input',
  INTERNAL_SERVER_ERROR: 'error_internal_server_error'
};

export function translateBackendError(errorPayload: any, t: Function): string {
  if (!errorPayload) return '';

  let errorCode = '';
  let fallbackMsg = '';

  if (typeof errorPayload === 'object' && errorPayload !== null) {
    errorCode = errorPayload.errorCode || '';
    fallbackMsg = errorPayload.message || '';
  } else if (typeof errorPayload === 'string') {
    errorCode = errorPayload;
    fallbackMsg = errorPayload;
  }

  if (errorCode && ERROR_CODE_TO_I18N_KEY[errorCode]) {
    const i18nKey = ERROR_CODE_TO_I18N_KEY[errorCode];
    const translated = t(i18nKey, { details: fallbackMsg });
    if (translated && translated !== i18nKey) {
      return translated;
    }
  }

  return fallbackMsg || errorCode;
}
