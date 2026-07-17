export function translateBackendError(errorMsg: string, t: Function): string {
  if (!errorMsg || typeof errorMsg !== 'string') return errorMsg;

  if (errorMsg.includes('Domain is missing required field mappings')) {
    return t('error_domain_missing_id');
  }
  if (errorMsg.includes('Data Quality Check Failed:')) {
    const details = errorMsg.replace('Data Quality Check Failed:', '').trim();
    return t('error_dq_failed', { details });
  }
  if (errorMsg.includes('Deduplication Failed') || errorMsg.includes('Duplicate found')) {
    const match = errorMsg.match(/Identifier Field \((.*?)\)/);
    if (match) {
      return t('error_dedup_failed', { field: match[1] });
    }
  }
  if (errorMsg.includes('Cannot update a record that is pending creation approval')) {
    return t('error_update_pending_creation');
  }
  if (errorMsg.includes('This record is already under a pending update approval')) {
    return t('error_update_pending_update');
  }
  if (errorMsg.includes('Cannot delete a record that is pending creation approval')) {
    return t('error_delete_pending_creation');
  }
  if (errorMsg.includes('You are not the assignee for this step')) {
    return t('error_not_assignee');
  }
  if (errorMsg.includes('Step is not pending')) {
    return t('error_step_not_pending');
  }
  if (errorMsg.includes('Username already exists')) {
    return t('error_username_exists');
  }
  if (errorMsg.includes('Invalid credentials')) {
    return t('error_invalid_credentials');
  }
  if (errorMsg.includes('Node does not belong to the specified domain')) {
    return t('error_node_domain_mismatch');
  }
  if (errorMsg.includes('Field does not belong to the specified node')) {
    return t('error_field_node_mismatch');
  }
  if (errorMsg.includes('Field does not belong to the specified domain')) {
    return t('error_field_domain_mismatch');
  }
  if (errorMsg.includes('Sector does not belong to the domain')) {
    return t('error_sector_domain_mismatch');
  }
  if (errorMsg.includes('Could not create the directory')) {
    return t('error_upload_dir_fail');
  }
  if (errorMsg.includes('Could not store file')) {
    return t('error_upload_file_fail');
  }

  return errorMsg;
}
