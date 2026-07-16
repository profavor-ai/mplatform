<template>
  <div class="records-container records-layout">
    <!-- Left Column: Classification Tree -->
    <div class="left-tree records-tree-column">
      <h3 style="padding: 0.5rem; margin: 0; border-bottom: 1px solid #ddd; font-size: 1rem; font-weight: bold; color: #555; text-transform: uppercase;">
        Classification Tree
      </h3>
      <div style="flex: 1; overflow-y: auto;">
        <va-card flat>
          <va-card-content style="padding: 0;">
            <div v-if="!treeNodes || treeNodes.length === 0" style="padding: 2rem; text-align: center; color: #666;">
              분류체계 트리가 없습니다.
            </div>
            <div v-else class="va-tree" style="width: 100%;">
              <SchemaTreeNode 
                v-for="domain in treeNodes" 
                :key="domain.id" 
                :node="domain"
                :selectedNode="selectedNode"
                :showEdit="false"
                @select="selectNode"
              />
            </div>
          </va-card-content>
        </va-card>
      </div>
    </div>

    <!-- Right Column: Record List & Data Grid -->
    <div class="right-content records-detail-column">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
        <div style="flex: 1; display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap;">
          <va-button v-if="searchableFields.length > 0" preset="secondary" icon="filter_list" @click="showAdvancedSearch = !showAdvancedSearch">
            상세 검색
          </va-button>
          <va-chip v-for="(val, key) in activeFilters" :key="key" v-show="val" color="primary" outline icon-right="close" @click:icon-right="removeFilter(key)">
            {{ getFilterFieldLabel(key) }}: {{ formatFilterValue(key, val) }}
          </va-chip>
        </div>
        <template v-if="selectedNode && !selectedNode.isDomain">
          <va-button color="primary" @click="openCreateModal">
            <va-icon name="add" class="mr-2"/> Create Record
          </va-button>
          <va-button color="success" outline @click="showExcelUploader = true" class="ml-2">
            <va-icon name="upload" class="mr-2"/> Bulk Upload
          </va-button>
        </template>
        <va-button v-else-if="selectedNode && selectedNode.isDomain" color="secondary" outline disabled>
          <va-icon name="info" class="mr-2"/> 하위 분류 노드를 선택해야 데이터를 생성할 수 있습니다
        </va-button>
      </div>

      <!-- Advanced Search Panel -->
      <va-card v-if="showAdvancedSearch" class="mb-4" style="background-color: #f8f9fa;">
        <va-card-content>
          <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 1.5rem; align-items: start;">
            <div v-for="field in searchableFields" :key="field.id" style="display: flex; flex-direction: column; gap: 0.4rem;">
              <span style="font-size: 0.75rem; color: var(--va-text-secondary); font-weight: 600; text-transform: uppercase;">{{ getTranslatedName(field.name) }}</span>
              
              <va-select
                v-if="['SELECT', 'MULTI_SELECT'].includes(field.type)"
                v-model="draftFilters[field.key]"
                :options="parseOptions(field.options)"
                value-by="value"
                placeholder="선택해주세요"
                clearable
                class="w-full"
              />
              <va-select
                v-else-if="field.type === 'BOOLEAN'"
                v-model="draftFilters[field.key]"
                :options="['true', 'false']"
                placeholder="선택해주세요"
                clearable
                class="w-full"
              />
              <div v-else-if="['NUMBER', 'DECIMAL', 'FLOAT', 'INTEGER'].includes(field.type)" style="display: flex; flex-direction: column; gap: 0.4rem; width: 100%;">
                <va-input
                  v-model="draftFilters[field.key]"
                  type="number"
                  placeholder="숫자 입력"
                  clearable
                  class="w-full"
                  @keyup.enter="applyFilters"
                >
                  <template #prependInner>
                    <select 
                      v-model="draftFiltersOp[field.key]" 
                      @click.stop
                      @mousedown.stop
                      style="border: none; outline: none; background: transparent; font-weight: bold; color: var(--va-primary); cursor: pointer; padding-right: 0.2rem; margin-right: 0.5rem; border-right: 1px solid var(--va-background-border);"
                    >
                      <option value="EQ">=</option>
                      <option value="GT">&gt;</option>
                      <option value="GTE">&gt;=</option>
                      <option value="LT">&lt;</option>
                      <option value="LTE">&lt;=</option>
                      <option value="BETWEEN">범위</option>
                    </select>
                  </template>
                </va-input>
                <va-input
                  v-if="draftFiltersOp[field.key] === 'BETWEEN'"
                  v-model="draftFiltersMax[field.key]"
                  type="number"
                  placeholder="최대값 (Max)"
                  clearable
                  class="w-full"
                  @keyup.enter="applyFilters"
                >
                  <template #prependInner>
                    <span style="font-weight: bold; color: #666; margin-right: 0.5rem; border-right: 1px solid #ccc; padding-right: 0.5rem;">~ 이하</span>
                  </template>
                </va-input>
              </div>
              <va-input
                v-else
                v-model="draftFilters[field.key]"
                placeholder="검색어 입력"
                clearable
                class="w-full"
                @keyup.enter="applyFilters"
              />
            </div>
          </div>
          <div style="display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1rem;">
            <va-button preset="secondary" @click="clearFilters">초기화</va-button>
            <va-button @click="applyFilters">검색</va-button>
          </div>
        </va-card-content>
      </va-card>
      
      <div style="flex: 1; display: flex; flex-direction: column; min-height: 0;">
        <va-card v-if="selectedNode" style="width: 100%; flex: 1; display: flex; flex-direction: column; min-height: 0;">
          <va-card-content style="padding: 0; flex: 1; display: flex; flex-direction: column; min-height: 0;">
            <div class="records-grid-wrapper">
              <ag-grid-vue
                style="width: 100%; height: 100%;"
                :theme="gridTheme"
                :autoSizeStrategy="autoSizeStrategy"
                :columnDefs="columnDefs"
                :defaultColDef="defaultColDef"
                rowModelType="infinite"
                :cacheBlockSize="20"
                rowSelection="single"
                :pagination="true"
                :paginationPageSize="20"
                :paginationPageSizeSelector="[10, 20, 50]"
                @grid-ready="onGridReady"
                @rowDoubleClicked="onRowDoubleClicked"
              />
            </div>
          </va-card-content>
        </va-card>
        
        <va-card v-else>
          <va-card-content style="text-align: center; padding: 3rem; color: #666;">
            Select a Classification Node from the tree to view or manage records.
          </va-card-content>
        </va-card>
      </div>
    </div>

    <ExcelUploader
      v-if="showExcelUploader"
      :nodeId="selectedNode?.id"
      :nodeFields="nodeFields"
      :domainReferences="domainReferences"
      @close="showExcelUploader = false"
      @uploaded="handleExcelUploaded"
    />

    <!-- Create Record Modal -->
    <va-modal v-model="showCreateModal" :title="`Create Record in ${selectedNode?.label}`" hide-default-actions>
      <div style="max-height: 60vh; overflow-y: auto; overflow-x: hidden; padding: 1rem; box-sizing: border-box; width: 100%;">
        <div v-if="!hasCreateWorkflow" style="margin-bottom: 1rem; padding: 0.5rem; background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; border-radius: 4px; text-align: center; font-weight: bold;">
          This classification node does not have a CREATE workflow configured. You cannot save records.
        </div>
        
        <!-- Sector Tabs -->
        <va-tabs v-model="activeSectorTab" style="margin-bottom: 1rem;">
          <template #tabs>
            <va-tab v-for="(sector, idx) in groupedFieldsArray" :key="sector.key" :name="idx">
              {{ sector.label }}
            </va-tab>
          </template>
        </va-tabs>
        
        <!-- Sector Content -->
        <div v-for="(sector, idx) in groupedFieldsArray" :key="sector.key" v-show="activeSectorTab === idx">
          <va-accordion multiple style="width: 100%;" class="mb-4">
            <va-collapse 
              v-for="(group, gIdx) in sector.groups" 
              :key="group.key"
              :header="group.label"
              v-model="group.isOpen"
              solid
              color="background-element"
              style="margin-bottom: 0.5rem;"
            >
              <div class="row" style="padding: 0.5rem; --va-input-wrapper-min-height: 28px; --va-input-font-size: 0.9rem; row-gap: 1.25rem;">
                <div v-for="field in group.fields" :key="field.id" :class="['flex', 'xs12', 'md' + (field.gridWidth || 12)]" style="display: flex; flex-direction: column; gap: 0.25rem;">
                    <!-- Unified External Label -->
                    <span style="font-size: 0.75rem; color: var(--va-text-secondary); font-weight: 600; text-transform: uppercase;">
                      {{ getTranslatedName(field.name) }}{{ field.required ? ' *' : '' }}{{ field.type === 'CALCULATED' ? ' (계산됨)' : '' }}
                    </span>

                    <!-- Text / Number -->
                    <va-input 
                      v-if="['TEXT', 'NUMBER', 'DECIMAL', 'FLOAT', 'INTEGER'].includes(field.type)" 
                      v-model="recordFormData[field.key]" 
                      :type="['NUMBER', 'DECIMAL', 'FLOAT', 'INTEGER'].includes(field.type) ? 'number' : 'text'"
                      class="w-full"
                    />
                    
                    <!-- Multilingual -->
                    <div v-else-if="field.type === 'MULTILINGUAL'" class="w-full" style="display: flex; gap: 0.5rem; flex-direction: row;">
                      <va-input v-model="recordFormData[field.key].ko" style="flex: 1;" class="slim-multilingual-input">
                        <template #prependInner><span style="font-size: 0.75rem; color: #888; font-weight: 600; margin-right: 0.5rem; border-right: 1px solid #ddd; padding-right: 0.5rem; white-space: nowrap;">한국어</span></template>
                      </va-input>
                      <va-input v-model="recordFormData[field.key].en" style="flex: 1;" class="slim-multilingual-input">
                        <template #prependInner><span style="font-size: 0.75rem; color: #888; font-weight: 600; margin-right: 0.5rem; border-right: 1px solid #ddd; padding-right: 0.5rem; white-space: nowrap;">English</span></template>
                      </va-input>
                    </div>
                    <!-- Calculated -->
                    <va-input 
                      v-else-if="field.type === 'CALCULATED'" 
                      v-model="recordFormData[field.key]" 
                      readonly
                      class="w-full"
                      style="background-color: #f4f6f8;"
                    />
                  
                    <!-- Select -->
                    <va-select 
                      v-else-if="['SELECT', 'MULTI_SELECT'].includes(field.type)" 
                      v-model="recordFormData[field.key]" 
                      :options="parseOptions(field.options)" 
                      :multiple="field.type === 'MULTI_SELECT' || field.isMultiValue"
                      value-by="value"
                      class="w-full"
                    />
                    
                    <!-- Domain Reference -->
                    <div v-else-if="field.type === 'DOMAIN_REFERENCE'" class="w-full" style="display: flex; gap: 0.5rem; align-items: center;">
                      <va-input 
                        :model-value="getDomainRefDisplayName(field.key, recordFormData[field.key])" 
                        readonly
                        style="flex: 1;"
                      />
                      <va-button icon="search" @click="openDomainRefModal(field.key, true)" />
                    </div>
  
                    <!-- Checkbox / Boolean -->
                    <va-checkbox
                      v-else-if="field.type === 'BOOLEAN'"
                      v-model="recordFormData[field.key]"
                      class="w-full"
                    />

                    <!-- File Upload -->
                    <div v-else-if="field.type === 'FILE'" class="w-full">
                      <va-file-upload v-model="recordFormData[field.key]" :type="field.isMultiValue ? 'list' : 'single'" dropzone class="w-full file-upload-wrapper">
                        <div style="display: flex; flex-direction: row; align-items: center; gap: 1rem; padding: 0.5rem; justify-content: center; width: 100%;">
                          <span style="font-size: 0.9rem; color: #666;">여기로 파일을 드래그 하거나</span>
                          <va-button size="small">내 PC에서 선택</va-button>
                        </div>
                      </va-file-upload>
                      <transition-group name="flip-list" tag="div" v-if="recordFormData[field.key] && recordFormData[field.key].length > 0" class="custom-file-list" @dragover.prevent>
                        <div 
                          v-for="(fileObj, i) in recordFormData[field.key]" 
                          :key="fileObj.url || fileObj.name" 
                          class="custom-file-item"
                          :draggable="field.isMultiValue"
                          @dragstart="onDragStart($event, i, recordFormData[field.key])"
                          @dragenter.prevent="onDragEnter($event, i, recordFormData[field.key])"
                          @dragover.prevent
                          @drop.prevent="onDrop($event, i, recordFormData[field.key])"
                          @dragend="onDragEnd($event)"
                          :style="field.isMultiValue ? 'cursor: grab;' : ''"
                        >
                          <div class="custom-file-info" style="display: flex; align-items: center;">
                            <va-icon v-if="field.isMultiValue" name="drag_indicator" style="color: #666; margin-right: 8px; cursor: grab;" />
                            {{ fileObj.name || extractFilename(fileObj.url || fileObj) }}
                          </div>
                          <div class="custom-file-actions">
                            <va-icon name="delete" style="cursor: pointer; color: #E53935;" @click="removeFile(recordFormData[field.key], i)" />
                          </div>
                        </div>
                      </transition-group>
                    </div>
                  </div>
                </div>
              </va-collapse>
          </va-accordion>
        </div>
      </div>
      <div style="display: flex; justify-content: flex-end; margin-top: 1rem; gap: 0.5rem;">
        <va-button color="success" :disabled="!hasCreateWorkflow" @click="promptDraftComment('CREATE')">Create & Submit for Approval</va-button>
        <va-button preset="secondary" @click="showCreateModal = false">Cancel</va-button>
      </div>
    </va-modal>

    <!-- Draft Comment Modal -->
    <va-modal v-model="showDraftCommentModal" title="상신 의견 작성" ok-text="상신" cancel-text="취소" @ok="executePendingSave">
      <div style="padding: 1rem;">
        <p style="margin-bottom: 1rem; color: #555;">(선택사항) 결재권자에게 남길 기안 의견을 작성해 주세요.</p>
        <va-input 
          v-model="draftCommentText" 
          type="textarea"
          placeholder="의견을 입력하세요..." 
          style="width: 100%;"
        />
      </div>
    </va-modal>

    <!-- Record Detail Modal -->
    <va-modal v-model="showDetailModal" :title="isSnapshotMode ? `Record Snapshot - ${selectedNode?.label}` : `Record Details - ${selectedNode?.label}`" hide-default-actions>
      <div style="max-height: 60vh; overflow-y: auto; overflow-x: hidden; padding: 1rem; box-sizing: border-box; width: 100%;">
        <div v-if="isSnapshotMode" style="margin-bottom: 1rem; padding: 0.5rem; background-color: #fff3cd; color: #856404; border: 1px solid #ffeeba; border-radius: 4px; text-align: center; font-weight: bold;">
          이전 데이터 스냅샷을 조회 중입니다. (읽기 전용)
        </div>
        <div v-if="hasPendingUpdate" style="margin-bottom: 1rem; padding: 0.5rem; background-color: #fff3cd; color: #856404; border: 1px solid #ffeeba; border-radius: 4px; text-align: center; font-weight: bold;">
          ⚠️ 이 레코드는 현재 변경 결재가 진행 중이므로 수정할 수 없습니다.
        </div>
        <div v-if="isEditingRecord && !hasUpdateWorkflow" style="margin-bottom: 1rem; padding: 0.5rem; background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; border-radius: 4px; text-align: center; font-weight: bold;">
          This classification node does not have an UPDATE workflow configured. You cannot save records.
        </div>
        <!-- Sector Tabs -->
        <va-tabs v-model="activeSectorTab" style="margin-bottom: 1rem;">
          <template #tabs>
            <va-tab v-for="(sector, idx) in groupedFieldsArray" :key="sector.key" :name="idx">
              {{ sector.label }}
            </va-tab>
          </template>
        </va-tabs>
        
        <!-- Sector Content -->
        <div v-for="(sector, idx) in groupedFieldsArray" :key="sector.key" v-show="activeSectorTab === idx">
          <va-accordion multiple style="width: 100%;" class="mb-4">
            <va-collapse 
              v-for="(group, gIdx) in sector.groups" 
              :key="group.key"
              :header="group.label"
              v-model="group.isOpen"
              solid
              color="background-element"
              style="margin-bottom: 0.5rem;"
            >
              <div style="padding: 0.5rem 1rem; display: flex; flex-direction: column; gap: 0.5rem; --va-input-wrapper-min-height: 28px; --va-input-font-size: 0.9rem;">
                  <div v-for="field in group.fields" :key="field.id" style="width: 100%; box-sizing: border-box; display: flex; flex-direction: column; gap: 0.25rem;">
                    <span style="font-size: 0.75rem; color: var(--va-text-secondary); font-weight: 600; text-transform: uppercase;">{{ getTranslatedName(field.name) }}{{ field.type === 'CALCULATED' ? ' (계산됨)' : '' }}</span>
                      <va-input 
                        v-if="['NUMBER', 'DECIMAL', 'FLOAT', 'INTEGER'].includes(field.type)" 
                        v-model="selectedRecordData[field.key]" 
                        type="number"
                        :readonly="!isEditingRecord"
                      />
                      <div v-else-if="field.type === 'DOMAIN_REFERENCE'" style="display: flex; gap: 0.5rem; align-items: center;">
                        <va-input 
                          :model-value="getDomainRefDisplayName(field.key, selectedRecordData[field.key])" 
                          readonly
                          style="flex: 1;"
                        />
                        <va-button v-if="isEditingRecord" icon="search" @click="openDomainRefModal(field.key, false)" />
                      </div>
                      <!-- Multilingual Edit -->
                      <div v-else-if="field.type === 'MULTILINGUAL'" class="w-full" style="display: flex; gap: 0.5rem; flex-direction: row;">
                        <va-input v-model="selectedRecordData[field.key].ko" style="flex: 1;" :readonly="!isEditingRecord" class="slim-multilingual-input">
                          <template #prependInner><span style="font-size: 0.75rem; color: #888; font-weight: 600; margin-right: 0.5rem; border-right: 1px solid #ddd; padding-right: 0.5rem; white-space: nowrap;">한국어</span></template>
                        </va-input>
                        <va-input v-model="selectedRecordData[field.key].en" style="flex: 1;" :readonly="!isEditingRecord" class="slim-multilingual-input">
                          <template #prependInner><span style="font-size: 0.75rem; color: #888; font-weight: 600; margin-right: 0.5rem; border-right: 1px solid #ddd; padding-right: 0.5rem; white-space: nowrap;">English</span></template>
                        </va-input>
                      </div>
                      <va-input 
                        v-else-if="field.type === 'CALCULATED'"
                        v-model="selectedRecordData[field.key]"
                        readonly
                        style="background-color: #f4f6f8;"
                      />
                      <va-select 
                        v-else-if="['SELECT', 'MULTI_SELECT'].includes(field.type)" 
                        v-model="selectedRecordData[field.key]" 
                        :options="parseOptions(field.options)" 
                        :multiple="field.type === 'MULTI_SELECT' || field.isMultiValue"
                        value-by="value"
                        class="w-full"
                        :readonly="!isEditingRecord"
                      />
                      <va-checkbox
                        v-else-if="field.type === 'BOOLEAN'"
                        v-model="selectedRecordData[field.key]"
                        class="w-full"
                        :readonly="!isEditingRecord"
                      />
                      <div v-else-if="field.type === 'FILE'" class="w-full">
                        <div v-if="!isEditingRecord" style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
                          <template v-if="selectedRecordData[field.key] && selectedRecordData[field.key].length > 0">
                            <va-chip 
                              v-for="(fileObj, i) in selectedRecordData[field.key]" 
                              :key="i" 
                              :href="fileObj.url || fileObj" 
                              target="_blank" 
                              outline 
                              icon="download"
                              color="primary"
                              style="cursor: pointer;"
                            >
                              {{ fileObj.name || extractFilename(fileObj.url || fileObj) }}
                            </va-chip>
                          </template>
                          <span v-else>-</span>
                        </div>
                        <va-file-upload v-else v-model="selectedRecordData[field.key]" :type="field.isMultiValue ? 'list' : 'single'" dropzone class="w-full file-upload-wrapper">
                          <div style="display: flex; flex-direction: row; align-items: center; gap: 1rem; padding: 0.5rem; justify-content: center; width: 100%;">
                            <span style="font-size: 0.9rem; color: #666;">여기로 파일을 드래그 하거나</span>
                            <va-button size="small">내 PC에서 선택</va-button>
                          </div>
                        </va-file-upload>
                        <transition-group name="flip-list" tag="div" v-if="isEditingRecord && selectedRecordData[field.key] && selectedRecordData[field.key].length > 0" class="custom-file-list" @dragover.prevent>
                          <div 
                            v-for="(fileObj, i) in selectedRecordData[field.key]" 
                            :key="fileObj.url || fileObj.name" 
                            class="custom-file-item"
                            :draggable="field.isMultiValue"
                            @dragstart="onDragStart($event, i, selectedRecordData[field.key])"
                            @dragenter.prevent="onDragEnter($event, i, selectedRecordData[field.key])"
                            @dragover.prevent
                            @drop.prevent="onDrop($event, i, selectedRecordData[field.key])"
                            @dragend="onDragEnd($event)"
                            :style="field.isMultiValue ? 'cursor: grab;' : ''"
                          >
                            <div class="custom-file-info" style="display: flex; align-items: center;">
                              <va-icon v-if="field.isMultiValue" name="drag_indicator" style="color: #666; margin-right: 8px; cursor: grab;" />
                              {{ fileObj.name || extractFilename(fileObj.url || fileObj) }}
                            </div>
                            <div class="custom-file-actions">
                              <va-icon name="delete" style="cursor: pointer; color: #E53935;" @click="removeFile(selectedRecordData[field.key], i)" />
                            </div>
                          </div>
                        </transition-group>
                      </div>
                      <va-input 
                        v-else
                        v-model="selectedRecordData[field.key]" 
                        type="text"
                        :readonly="!isEditingRecord"
                      />
                  </div>
                </div>
              </va-collapse>
          </va-accordion>
        </div>
      </div>
      <div style="display: flex; justify-content: flex-end; margin-top: 1rem; gap: 0.5rem;">
        <va-button v-if="!isEditingRecord && !isSnapshotMode && !hasPendingUpdate" color="danger" @click="requestDeleteRecord">Delete</va-button>
        <va-button v-if="!isEditingRecord && !isSnapshotMode && !hasPendingUpdate" color="warning" @click="isEditingRecord = true">Edit</va-button>
        <va-button v-if="!isEditingRecord && !isSnapshotMode" color="info" @click="openHistory">History</va-button>
        <va-button v-if="isEditingRecord && !isSnapshotMode" color="success" :disabled="!hasUpdateWorkflow" @click="promptDraftComment('UPDATE')">Save</va-button>
        <va-button @click="showDetailModal = false">Close</va-button>
      </div>
    </va-modal>

    <!-- Record History Modal -->
    <va-modal v-model="showHistoryModal" title="Record History" hide-default-actions size="large">
      <div style="max-height: 60vh; overflow-y: auto; padding: 1rem; box-sizing: border-box; width: 100%;">
        <div v-if="!historyLogs || historyLogs.length === 0" style="text-align: center; color: #777;">
          이력 데이터가 없습니다.
        </div>
        <div v-else>
          <va-data-table
            :items="historyLogs"
            :columns="historyColumns"
            striped
            hoverable
          >
            <template #cell(changedAt)="{ value }">
              <span style="white-space: nowrap;">{{ formatDate(value) }}</span>
            </template>
            <template #cell(changedBy)="{ value }">
              <span style="white-space: nowrap;">{{ getUserName(value) }}</span>
            </template>
            <template #cell(changeType)="{ value }">
              <va-badge
                v-if="value === 'PENDING_APPROVAL'"
                color="warning"
                text="결재 진행중"
              />
              <va-badge
                v-else
                :color="value === 'CREATE' ? 'success' : (value === 'DELETE' ? 'danger' : 'info')"
                :text="value"
              />
            </template>
            <template #cell(diff)="{ row }">
              <div v-if="row.rowData.changeType === 'PENDING_APPROVAL' && row.rowData.rawRequest" style="padding: 0.25rem 0;">
                <va-button size="small" outline @click="viewDiffDetails(row.rowData.rawRequest.changes, row.rowData.rawRequest.targetType, true)">변경 내역 보기</va-button>
                <div style="margin-top: 0.25rem; font-size: 0.85rem; font-weight: bold; color: var(--va-primary); display: flex; align-items: center; gap: 0.25rem;">
                  <va-icon name="hourglass_empty" size="small" />
                  <span>대기중: {{ getUserName(row.rowData.rawRequest.steps.find(s => s.status === 'PENDING')?.assigneeId) }}</span>
                </div>
              </div>
              <div v-else-if="row.rowData.changeType === 'UPDATE'" style="padding: 0.25rem 0;">
                <va-button size="small" outline @click="viewDiffDetails(row.rowData.previousData, row.rowData.newData, false)">변경 내역 보기</va-button>
              </div>
              <div v-else-if="row.rowData.changeType === 'CREATE'" style="color: var(--va-success); font-size: 0.85rem; font-weight: bold; display: flex; align-items: center; gap: 0.5rem;">
                <va-badge color="success" text="CREATE" size="small" /> 초기 생성됨
              </div>
              <div v-else-if="row.rowData.changeType === 'DELETE'" style="color: #c62828; font-size: 0.85rem; font-weight: bold; display: flex; align-items: center; gap: 0.5rem;">
                <va-badge color="danger" text="DELETE" size="small" /> 삭제됨
              </div>
            </template>
            <template #cell(actions)="{ row }">
              <div v-if="row.rowData.changeType === 'CREATE'" style="display: flex; gap: 0.5rem;">
                <va-button size="small" color="info" outline @click="viewSnapshot(row.rowData.newData)">스냅샷 보기</va-button>
                <va-button v-if="row.rowData.approvalRequestId" size="small" color="secondary" outline @click="viewApprovalHistory(row.rowData)">결재 내역</va-button>
              </div>
              <div v-else-if="row.rowData.changeType === 'DELETE'" style="display: flex; gap: 0.5rem;">
                <va-button size="small" color="warning" outline @click="viewSnapshot(row.rowData.previousData)">마지막 스냅샷</va-button>
                <va-button v-if="row.rowData.approvalRequestId" size="small" color="secondary" outline @click="viewApprovalHistory(row.rowData)">결재 내역</va-button>
              </div>
              <div v-else-if="row.rowData.changeType === 'UPDATE'" style="display: flex; gap: 0.5rem; flex-wrap: nowrap;">
                <va-button size="small" color="warning" outline @click="viewSnapshot(row.rowData.previousData)">이전 스냅샷</va-button>
                <va-button size="small" color="info" outline @click="viewSnapshot(row.rowData.newData)">이후 스냅샷</va-button>
                <va-button v-if="row.rowData.approvalRequestId" size="small" color="secondary" outline @click="viewApprovalHistory(row.rowData)">결재 내역</va-button>
              </div>
              <div v-else-if="row.rowData.changeType === 'PENDING_APPROVAL'" style="display: flex; gap: 0.5rem;">
                <va-button size="small" color="warning" @click="viewApprovalHistory(row.rowData)">결재 모니터링</va-button>
              </div>
            </template>
          </va-data-table>
        </div>
      </div>
      <div style="display: flex; justify-content: flex-end; margin-top: 1rem;">
        <va-button @click="showHistoryModal = false">Close</va-button>
      </div>
    </va-modal>

    <!-- Record Snapshot Modal (New Modal) -->
    <va-modal v-model="showSnapshotModal" :title="`Record Snapshot - ${selectedNode?.label}`" hide-default-actions>
      <div style="max-height: 60vh; overflow-y: auto; overflow-x: hidden; padding: 1rem; box-sizing: border-box; width: 100%;">
        <div style="margin-bottom: 1rem; padding: 0.5rem; background-color: #fff3cd; color: #856404; border: 1px solid #ffeeba; border-radius: 4px; text-align: center; font-weight: bold;">
          이전 데이터 스냅샷을 조회 중입니다. (읽기 전용)
        </div>
        
        <!-- Sector Tabs -->
        <va-tabs v-model="activeSnapshotSectorTab" style="margin-bottom: 1rem;">
          <template #tabs>
            <va-tab v-for="(sector, idx) in groupedFieldsArray" :key="sector.key" :name="idx">
              {{ sector.label }}
            </va-tab>
          </template>
        </va-tabs>
        
        <!-- Sector Content -->
        <div v-for="(sector, idx) in groupedFieldsArray" :key="sector.key" v-show="activeSnapshotSectorTab === idx">
          <va-accordion multiple style="width: 100%;" class="mb-4">
            <va-collapse 
              v-for="(group, groupIdx) in sector.groups" 
              :key="group.key"
              :header="group.label"
              v-model="group.isOpen"
              solid
              color="background-element"
              style="margin-bottom: 0.5rem;"
            >
              <div style="padding: 0.5rem 1rem; display: flex; flex-direction: column; gap: 0.5rem; --va-input-wrapper-min-height: 28px; --va-input-font-size: 0.9rem;">
                  <div v-for="field in group.fields" :key="field.id" style="width: 100%; box-sizing: border-box; display: flex; flex-direction: column; gap: 0.25rem;">
                    <span style="font-size: 0.75rem; color: var(--va-text-secondary); font-weight: 600; text-transform: uppercase;">{{ getTranslatedName(field.name) }}{{ field.type === 'CALCULATED' ? ' (계산됨)' : '' }}</span>
                      <va-input 
                        v-if="['NUMBER', 'DECIMAL', 'FLOAT', 'INTEGER'].includes(field.type)" 
                        :model-value="snapshotRecordData[field.key]" 
                        type="number"
                        readonly
                      />
                      <div v-else-if="field.type === 'DOMAIN_REFERENCE'" style="display: flex; gap: 0.5rem; align-items: center;">
                        <va-input 
                          :model-value="getDomainRefDisplayName(field.key, snapshotRecordData[field.key])" 
                          readonly
                          style="flex: 1;"
                        />
                      </div>
                      <!-- Multilingual -->
                      <div v-else-if="field.type === 'MULTILINGUAL'" class="w-full" style="display: flex; gap: 0.5rem; flex-direction: row;">
                        <va-input :model-value="snapshotRecordData[field.key]?.ko" style="flex: 1;" readonly class="slim-multilingual-input">
                          <template #prependInner><span style="font-size: 0.75rem; color: #888; font-weight: 600; margin-right: 0.5rem; border-right: 1px solid #ddd; padding-right: 0.5rem; white-space: nowrap;">한국어</span></template>
                        </va-input>
                        <va-input :model-value="snapshotRecordData[field.key]?.en" style="flex: 1;" readonly class="slim-multilingual-input">
                          <template #prependInner><span style="font-size: 0.75rem; color: #888; font-weight: 600; margin-right: 0.5rem; border-right: 1px solid #ddd; padding-right: 0.5rem; white-space: nowrap;">English</span></template>
                        </va-input>
                      </div>
                      <va-input 
                        v-else-if="field.type === 'CALCULATED'"
                        :model-value="snapshotRecordData[field.key]"
                        readonly
                        style="background-color: #f4f6f8;"
                      />
                      <va-select 
                        v-else-if="['SELECT', 'MULTI_SELECT'].includes(field.type)" 
                        :model-value="snapshotRecordData[field.key]" 
                        :options="parseOptions(field.options)" 
                        :multiple="field.type === 'MULTI_SELECT' || field.isMultiValue"
                        value-by="value"
                        class="w-full"
                        readonly
                      />
                      <va-checkbox
                        v-else-if="field.type === 'BOOLEAN'"
                        :model-value="snapshotRecordData[field.key]"
                        class="w-full"
                        readonly
                      />
                      <div v-else-if="field.type === 'FILE'" class="w-full">
                        <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
                          <template v-if="snapshotRecordData[field.key] && snapshotRecordData[field.key].length > 0">
                            <va-chip 
                              v-for="(fileObj, fileIdx) in snapshotRecordData[field.key]" 
                              :key="fileIdx" 
                              :href="fileObj.url || fileObj" 
                              target="_blank" 
                              outline 
                              icon="download"
                              color="primary"
                              style="cursor: pointer;"
                            >
                              {{ fileObj.name || extractFilename(fileObj.url || fileObj) }}
                            </va-chip>
                          </template>
                          <span v-else>-</span>
                        </div>
                      </div>
                      <va-input 
                        v-else
                        :model-value="snapshotRecordData[field.key]" 
                        type="text"
                        readonly
                      />
                  </div>
                </div>
            </va-collapse>
          </va-accordion>
        </div>
      </div>
      <div style="display: flex; justify-content: flex-end; margin-top: 1rem; gap: 0.5rem;">
        <va-button @click="showSnapshotModal = false">Close</va-button>
      </div>
    </va-modal>

    <!-- Diff Modal -->
    <va-modal v-model="showDiffModal" title="변경 내역 상세" hide-default-actions size="large">
      <div style="padding: 1rem; box-sizing: border-box; width: 100%; max-height: 60vh; overflow-y: auto;">
        <div v-if="!selectedDiffs || selectedDiffs.length === 0" style="color: #777; font-style: italic;">
          변경된 필드가 없습니다.
        </div>
        <div v-else style="display: flex; flex-direction: column; gap: 0.25rem;">
          <div v-for="diff in selectedDiffs" :key="diff.fieldName" style="display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; background: #f9f9f9; padding: 0.35rem 0.5rem; border-radius: 4px; border: 1px solid #eee; font-size: 0.85rem;">
            <span style="font-weight: bold; color: #333; min-width: 90px;">{{ diff.fieldName }}</span>
            <div style="flex: 1; display: flex; align-items: center; gap: 0.35rem; flex-wrap: wrap;">
              <template v-if="(diff.before === undefined || diff.before === null || diff.before === '' || diff.before === 'undefined')">
                <va-badge color="info" text="NEW" size="small" />
                <span style="color: #2c3e50; font-weight: bold; font-family: monospace; font-size: 0.8rem;">{{ diff.after }}</span>
              </template>
              <template v-else-if="(diff.after === undefined || diff.after === null || diff.after === '' || diff.after === 'undefined')">
                <va-badge color="danger" text="DEL" size="small" />
                <span style="color: #666; text-decoration: line-through; font-family: monospace; font-size: 0.8rem;">{{ diff.before }}</span>
              </template>
              <template v-else>
                <va-badge color="warning" text="MOD" size="small" />
                <span style="color: #666; text-decoration: line-through; font-family: monospace; font-size: 0.8rem;">{{ diff.before }}</span>
                <span style="color: #999; font-weight: bold;">&rarr;</span>
                <span style="color: #2c3e50; font-weight: bold; font-family: monospace; font-size: 0.8rem;">{{ diff.after }}</span>
              </template>
            </div>
          </div>
        </div>
      </div>
      <div style="display: flex; justify-content: flex-end; margin-top: 1rem;">
        <va-button @click="showDiffModal = false">Close</va-button>
      </div>
    </va-modal>

    <!-- Approval History Modal -->
    <va-modal v-model="showApprovalHistoryModal" title="결재 내역 상세" hide-default-actions size="large">
      <div style="max-height: 60vh; overflow-y: auto; padding: 1rem; box-sizing: border-box; width: 100%;">
        <div v-if="!selectedApprovalRequest" style="text-align: center; color: #777;">
          데이터를 불러오는 중입니다...
        </div>
        <div v-else>
          <ApprovalDetailsViewer :request="selectedApprovalRequest" />
        </div>
      </div>
      <div style="display: flex; justify-content: flex-end; margin-top: 1rem;">
        <va-button @click="showApprovalHistoryModal = false">Close</va-button>
      </div>
    </va-modal>

    <!-- Domain Reference Search Modal -->
    <va-modal v-model="showDomainRefModal" title="Select Reference Record" hide-default-actions size="large">
      <div style="height: 50vh; width: 100%; display: flex; flex-direction: column;">
        <div style="margin-bottom: 1rem; color: #666; font-size: 0.9rem;">
          원하시는 레코드를 목록에서 더블 클릭하여 선택해 주세요.
        </div>
        <div style="flex: 1; width: 100%;">
          <AgGridVue
            style="width: 100%; height: 100%;"
            :theme="gridTheme"
            :autoSizeStrategy="autoSizeStrategy"
            :columnDefs="domainRefColDefs"
            :rowData="domainRefRowData"
            :defaultColDef="{ sortable: true, filter: true, resizable: true }"
            rowSelection="single"
            @rowDoubleClicked="onDomainRefRowDoubleClicked"
          />
        </div>
      </div>
      <div style="display: flex; justify-content: flex-end; margin-top: 1rem;">
        <va-button @click="showDomainRefModal = false">Cancel</va-button>
      </div>
    </va-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useCookie, useState } from '#app'
import { AgGridVue } from 'ag-grid-vue3'
import ExcelUploader from '~/components/ExcelUploader.vue'
import { useColors } from 'vuestic-ui'

const { gridTheme, autoSizeStrategy } = useAgGridTheme()

const { currentPresetName } = useColors()
const isDark = computed(() => currentPresetName.value === 'dark')

// Global i18n sync
const currentLocale = useCookie('locale', { default: () => 'ko' })
const token = useCookie('auth_token', { default: () => '' })

const userCookie = useCookie('user_data')
const currentUser = computed(() => {
  if (userCookie.value) {
    return typeof userCookie.value === 'string' ? JSON.parse(userCookie.value) : userCookie.value
  }
  return null
})

const userList = ref([])

const loadUsers = async () => {
  try {
    const res = await fetch('/api/auth/users', {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
    if (res.ok) {
      userList.value = await res.json()
    }
  } catch (e) {
    console.error("Failed to fetch users", e)
  }
}

const getUserName = (uuid) => {
  if (!uuid) return ''
  const u = userList.value.find(user => user.uuid === uuid)
  return u ? u.username : uuid
}

const treeNodes = ref([])
const selectedNode = ref(null)
const hasCreateWorkflow = ref(true)
const hasUpdateWorkflow = ref(true)

const nodeFields = ref([])
const rowData = ref([])
const domainReferences = ref({}) 
const showExcelUploader = ref(false)

const loadDomainReferences = async (fields) => {
  domainReferences.value = {}
  for (const f of fields) {
    if (f.type === 'DOMAIN_REFERENCE') {
      try {
        const opts = JSON.parse(f.options || '{}')
        const tDomainId = opts.targetDomainId
        if (!tDomainId) continue
        
        const domains = await $fetch('/api/domains', { headers: { Authorization: `Bearer ${token.value}` } })
        const tDomain = domains.find(d => d.id === tDomainId)
        
        const tFields = await $fetch(`/api/domains/${tDomainId}/fields`, { headers: { Authorization: `Bearer ${token.value}` } })
        const tRecords = await $fetch(`/api/records/domain/${tDomainId}`, { headers: { Authorization: `Bearer ${token.value}` } })
        
        domainReferences.value[f.key] = {
          targetDomainId: tDomainId,
          domainInfo: tDomain,
          fields: tFields,
          records: tRecords
        }
      } catch (e) {
        console.error('Failed to load domain reference info for field:', f.key, e)
      }
    }
  }
}

const showDomainRefModal = ref(false)
const domainRefColDefs = ref([])
const domainRefRowData = ref([])
const currentDomainRefFieldKey = ref(null)
const isDomainRefForCreate = ref(false)

const openDomainRefModal = (fieldKey, isCreate = false) => {
  const refInfo = domainReferences.value[fieldKey]
  if (!refInfo) {
    alert('Target domain reference info not loaded.')
    return
  }
  currentDomainRefFieldKey.value = fieldKey
  isDomainRefForCreate.value = isCreate
  const tDomain = refInfo.domainInfo
  
  const idField = refInfo.fields.find(f => f.id === tDomain.identifierFieldId)
  const nameField = refInfo.fields.find(f => f.id === tDomain.displayNameFieldId)
  const descField = refInfo.fields.find(f => f.id === tDomain.descriptionFieldId)
  
  const createColDef = (field) => {
    const def = {
      headerName: getTranslatedName(field.name),
      field: `data.${field.key}`,
    }
    if (field.tableColumnWidth) {
      def.width = field.tableColumnWidth
    } else {
      def.flex = 1
    }
    
    def.valueFormatter = (params) => {
      if (!params.value) return ''
      if (field.type === 'MULTILINGUAL') {
        try {
          const obj = typeof params.value === 'string' ? JSON.parse(params.value) : params.value;
          return obj[currentLocale.value] || obj.ko || obj.en || JSON.stringify(params.value);
        } catch(e) { return String(params.value); }
      }
      if (typeof params.value === 'object') return JSON.stringify(params.value);
      return String(params.value);
    }
    return def;
  }

  const cols = []
  if (idField) cols.push(createColDef(idField))
  if (nameField) cols.push(createColDef(nameField))
  if (descField) cols.push(createColDef(descField))
  
  if (cols.length === 0) cols.push({ headerName: 'System ID', field: 'id', flex: 1 })
  
  domainRefColDefs.value = cols
  domainRefRowData.value = refInfo.records.map(r => ({
    id: r.id,
    data: typeof r.data === 'string' ? JSON.parse(r.data) : r.data
  }))
  showDomainRefModal.value = true
}

const onDomainRefRowDoubleClicked = (params) => {
  if (currentDomainRefFieldKey.value) {
    if (isDomainRefForCreate.value) {
      recordFormData.value[currentDomainRefFieldKey.value] = params.data.id
    } else {
      selectedRecordData.value[currentDomainRefFieldKey.value] = params.data.id
    }
  }
  showDomainRefModal.value = false
}


const domainRefDisplayMap = ref({})

const extractMultilingualField = (dataObj, fieldKey) => {
  if (!dataObj || !fieldKey) return null;
  const rawVal = dataObj[fieldKey];
  if (rawVal === null || rawVal === undefined || rawVal === '') return null;
  if (typeof rawVal === 'string') {
    try {
      const parsed = JSON.parse(rawVal);
      if (parsed && typeof parsed === 'object') {
        return parsed[currentLocale.value] || parsed.ko || parsed.en || rawVal;
      }
    } catch(e) {}
    return rawVal;
  } else if (typeof rawVal === 'object') {
    return rawVal[currentLocale.value] || rawVal.ko || rawVal.en || JSON.stringify(rawVal);
  }
  return String(rawVal);
}

const buildDomainRefDisplayString = (dataObj, tDomain, tFields) => {
  if (!dataObj || !tDomain || !tFields) return null;
  
  const idFieldId = tDomain.identifierFieldId;
  let dFieldId = tDomain.displayNameFieldId || tDomain.identifierFieldId;
  
  let idF = tFields.find(x => x.id === idFieldId);
  let nameF = tFields.find(x => x.id === dFieldId);
  if (!nameF) {
    nameF = tFields.find(x => {
      const n = JSON.stringify(x.name).toLowerCase();
      return n.includes('name') || n.includes('\uC774\uB984') || n.includes('\uC0AC\uC6D0\uBA85') || n.includes('title') || n.includes('\uC81C\uBAA9');
    });
    if (!nameF) nameF = tFields.find(x => x.type === 'TEXT');
  }
  
  const idStr = extractMultilingualField(dataObj, idF?.key);
  const nameStr = extractMultilingualField(dataObj, nameF?.key);
  
  if (idStr && nameStr && idStr !== nameStr) {
    return `[${idStr}] ${nameStr}`;
  } else if (nameStr) {
    return nameStr;
  } else if (idStr) {
    return `[${idStr}]`;
  }
  return null;
}

const fetchDomainRefName = async (uuid, targetDomainId) => {
  if (!uuid || domainRefDisplayMap.value[uuid]) return;
  domainRefDisplayMap.value[uuid] = 'Loading...';
  try {
    const rec = await $fetch(`/api/records/${uuid}`, { headers: { Authorization: `Bearer ${token.value}` } }).catch(() => null);
    if (!rec) {
      const uname = getUserName(uuid);
      domainRefDisplayMap.value[uuid] = (uname && uname !== uuid) ? uname : uuid;
      return;
    }
    
    let tDomainId = targetDomainId;
    if (!tDomainId && rec.node) {
      tDomainId = rec.node.domain?.id || rec.node.domainId;
    }
    if (!tDomainId) tDomainId = rec.domainId;
    
    if (!tDomainId) {
      domainRefDisplayMap.value[uuid] = uuid;
      return;
    }

    const domains = await $fetch('/api/domains', { headers: { Authorization: `Bearer ${token.value}` } });
    const tDomain = domains.find(d => d.id === tDomainId);
    if (!tDomain) {
      domainRefDisplayMap.value[uuid] = uuid;
      return;
    }
    
    const tFields = await $fetch(`/api/domains/${tDomainId}/fields`, { headers: { Authorization: `Bearer ${token.value}` } });
    
    if (rec.data) {
      const dataObj = typeof rec.data === 'string' ? JSON.parse(rec.data) : rec.data;
      const displayStr = buildDomainRefDisplayString(dataObj, tDomain, tFields);
      domainRefDisplayMap.value[uuid] = displayStr || uuid;
    } else {
      domainRefDisplayMap.value[uuid] = uuid;
    }
  } catch (e) {
    const uname = getUserName(uuid);
    domainRefDisplayMap.value[uuid] = (uname && uname !== uuid) ? uname : uuid;
  }
}

const getDomainRefDisplayName = (fieldKey, recordId) => {
  if (!recordId) return ''
  const refInfo = domainReferences.value[fieldKey]
  if (!refInfo) return recordId
  
  const record = refInfo.records.find(r => r.id === recordId)
  if (record) {
    const data = typeof record.data === 'string' ? JSON.parse(record.data) : record.data;
    const displayStr = buildDomainRefDisplayString(data, refInfo.domainInfo, refInfo.fields);
    if (displayStr) return displayStr;
    return recordId;
  }
  
  if (!domainRefDisplayMap.value[recordId]) {
    fetchDomainRefName(recordId, refInfo.targetDomainId);
  }
  return domainRefDisplayMap.value[recordId] || 'Loading...';
}

const gridApi = ref(null)

const showCreateModal = ref(false)
const activeSectorTab = ref(0)
const recordFormData = ref({})

const parseName = (nameObj) => {
  if (!nameObj) return null;
  if (typeof nameObj === 'string') {
    try {
      return JSON.parse(nameObj);
    } catch (e) {
      return null;
    }
  }
  return nameObj;
}

const getTranslatedName = (nameObj) => {
  const pName = parseName(nameObj)
  return pName?.[currentLocale.value] || pName?.ko || pName?.en || ''
}

const groupedFieldsArray = computed(() => {
  const map = new Map()
  
  const sortedFields = [...nodeFields.value].sort((a, b) => (a.order || 0) - (b.order || 0))

  sortedFields.forEach(f => {
    const sObj = f.fieldGroup?.sector
    const gObj = f.fieldGroup

    const sName = getTranslatedName(sObj?.name) || (currentLocale.value === 'ko' ? '일반' : 'General')
    const sKey = sObj?.id || 'default'
    const sOrder = sObj?.sortOrder || 0
    
    const gName = getTranslatedName(gObj?.name) || (currentLocale.value === 'ko' ? '기본 필드' : 'Fields')
    const gKey = gObj?.id || 'default'
    const gOrder = gObj?.sortOrder || 0
    
    if (!map.has(sKey)) {
      map.set(sKey, { key: sKey, label: sName, order: sOrder, groups: new Map() })
    }
    const sectorObj = map.get(sKey)
    
    if (!sectorObj.groups.has(gKey)) {
      sectorObj.groups.set(gKey, { key: gKey, label: gName, order: gOrder, fields: [], isOpen: gObj?.isDefaultOpen ?? true })
    }
    sectorObj.groups.get(gKey).fields.push(f)
  })
  
  const sectors = Array.from(map.values())
  sectors.sort((a, b) => a.order - b.order)
  
  return sectors.map(s => {
    const groups = Array.from(s.groups.values())
    groups.sort((a, b) => a.order - b.order)
    return {
      key: s.key,
      label: s.label,
      groups: groups
    }
  })
})

const parseOptions = (opts) => {
  if (!opts) return []
  if (typeof opts === 'string') {
    if (opts.trim().startsWith('[')) {
      try { 
        const parsed = JSON.parse(opts) 
        const mapped = parsed.map(o => {
          if (typeof o === 'string') return { text: o, value: o, order: 0 }
          return {
            value: o.key,
            text: o.label?.[currentLocale.value] || o.label?.ko || o.label?.en || o.key,
            order: o.order || 0
          }
        })
        return mapped.sort((a, b) => a.order - b.order)
      } catch(e){}
    }
    return opts.split(',').map(s => {
      const val = s.trim();
      return { text: val, value: val };
    })
  }
  return opts
}

const isMultiple = (field) => {
  if (!field.options) return false;
  try {
    const opts = JSON.parse(field.options);
    return opts.multiple === true;
  } catch (e) {
    return false;
  }
}

const loadTree = async () => {
  try {
    const domains = await $fetch('/api/domains', {
      headers: { Authorization: `Bearer ${token.value}` }
    })
    
    const builtTree = []
    for (const d of domains) {
      const nodes = await $fetch(`/api/domains/${d.id}/nodes/tree`, {
        headers: { Authorization: `Bearer ${token.value}` }
      })
      
      const formatNode = (n) => {
        const pName = parseName(n.name);
        return {
          id: n.id,
          label: pName?.[currentLocale.value] || pName?.ko || pName?.en || 'Unknown',
          domainId: d.id,
          isDomain: false,
          children: n.children ? n.children.map(formatNode) : [],
          originalNameMap: pName
        };
      };
      
      const dName = parseName(d.name);
      builtTree.push({
        id: d.id,
        label: (dName?.[currentLocale.value] || dName?.ko || dName?.en || 'Unknown') + ' (Domain)',
        domainId: d.id,
        isDomain: true,
        expanded: true,
        children: nodes.map(formatNode),
        originalNameMap: dName
      })
    }
    treeNodes.value = builtTree
  } catch (error) {
    console.error('Failed to load tree:', error.message || error)
  }
}

watch(currentLocale, () => {
  const updateLabel = (nodes) => {
    nodes.forEach(n => {
      if (n.originalNameMap) {
        n.label = n.originalNameMap[currentLocale.value] || n.originalNameMap.ko || n.originalNameMap.en || 'Unknown';
        if (n.isDomain) n.label += ' (Domain)';
      }
      if (n.children && n.children.length > 0) {
        updateLabel(n.children);
      }
    })
  }
  updateLabel(treeNodes.value)
  
  if (gridApi.value && columnDefs.value) {
    gridApi.value.setGridOption('columnDefs', buildColumnDefs(nodeFields.value))
  }
})

onMounted(() => {
  loadUsers()
  loadTree()
})

const selectNode = async (node) => {
  selectedNode.value = node || null
  
  if (!node) {
    nodeFields.value = []
    rowData.value = []
    columnDefs.value = []
    return
  }
  
  if (node.isDomain) {
    try {
      const fields = await $fetch(`/api/domains/${node.id}/fields`, {
        headers: { Authorization: `Bearer ${token.value}` }
      }).catch(() => [])
      nodeFields.value = fields
      await loadDomainReferences(fields)
      columnDefs.value = buildColumnDefs(fields, true)
      await fetchRecords()
    } catch (e) {
      console.error(e)
    }
    return
  }
  
  try {
    const fields = await $fetch(`/api/nodes/${node.id}/fields/effective`, {
      headers: { Authorization: `Bearer ${token.value}` }
    })
    nodeFields.value = fields
    await loadDomainReferences(fields)
    
    const isParentNode = node.children && node.children.length > 0;
    columnDefs.value = buildColumnDefs(fields, isParentNode)
    
    try {
      const [createRes, updateRes] = await Promise.all([
        $fetch(`/api/approval-requests/effective-workflow/${node.id}?actionType=CREATE`, { headers: { Authorization: `Bearer ${token.value}` } }).catch(() => true),
        $fetch(`/api/approval-requests/effective-workflow/${node.id}?actionType=UPDATE`, { headers: { Authorization: `Bearer ${token.value}` } }).catch(() => true)
      ])
      hasCreateWorkflow.value = createRes === true
      hasUpdateWorkflow.value = updateRes === true
    } catch(e) {
      hasCreateWorkflow.value = true
      hasUpdateWorkflow.value = true
    }
    
    await fetchRecords()
  } catch (e) {
    console.error(e)
  }
}

const buildColumnDefs = (fields, showNodeColumn = false) => {
  const defs = [
    { field: 'id', headerName: 'ID', sortable: true, width: 100 },
    { 
      field: 'nodeName', 
      headerName: 'Classification Node', 
      sortable: true, 
      width: 180,
      hide: !showNodeColumn 
    },
    { 
      field: 'status', 
      headerName: 'Status', 
      sortable: true, 
      width: 150,
      cellRenderer: (params) => {
        if (!params || params.value === undefined || params.value === null || params.value === '') {
          return '';
        }
        const color = params.value === 'ACTIVE' ? '#2c82e0' : (params.value === 'PENDING_APPROVAL' ? '#e6a23c' : '#f56c6c')
        const span = document.createElement('span');
        span.style.padding = '2px 8px';
        span.style.borderRadius = '4px';
        span.style.background = color;
        span.style.color = 'white';
        span.style.fontSize = '12px';
        span.style.fontWeight = 'bold';
        span.innerText = params.value;
        return span;
      }
    }
  ]
  
  const groupMap = {}

  fields.forEach(f => {
    const colDef = {
      headerName: getTranslatedName(f.name),
      field: `data.${f.key}`,
      valueGetter: (params) => {
        if (!params.data || !params.data.data) return null;
        if (params.data.data[f.key] !== undefined) return params.data.data[f.key];
        const lowerKey = String(f.key).toLowerCase();
        if (params.data.data[lowerKey] !== undefined) return params.data.data[lowerKey];
        const upperKey = String(f.key).toUpperCase();
        if (params.data.data[upperKey] !== undefined) return params.data.data[upperKey];
        return null;
      },
      sortable: true
    }
    if (f.tableColumnWidth) {
      colDef.width = f.tableColumnWidth
    } else {
      colDef.flex = 1
    }
    if (f.type === 'FILE') {
      colDef.cellRenderer = (params) => {
        if (!params || !params.value) return ''
        const getFilename = (url) => {
          try {
            if (url.includes('?name=')) return decodeURIComponent(url.split('?name=')[1].split('&')[0]);
            return decodeURIComponent(url.split('/').pop().split('?')[0]) || 'Download';
          } catch(e) { return 'Download'; }
        };

        const createLink = (url) => {
          const a = document.createElement('a');
          a.href = url;
          a.target = '_blank';
          a.style.color = 'blue';
          a.style.textDecoration = 'underline';
          a.innerText = getFilename(url);
          return a;
        };

        const container = document.createElement('div');
        try {
          const arr = JSON.parse(params.value)
          if (Array.isArray(arr)) {
            arr.forEach((url, index) => {
              if (index > 0) {
                container.appendChild(document.createElement('br'));
              }
              container.appendChild(createLink(url));
            });
            return container;
          }
        } catch(e) {}

        return createLink(params.value);
      }
    } else if (f.type === 'MULTILINGUAL') {
      colDef.cellRenderer = (params) => {
        if (!params.value) return ''
        try {
          const obj = typeof params.value === 'string' ? JSON.parse(params.value) : params.value;
          return obj[currentLocale.value] || obj.ko || obj.en || JSON.stringify(params.value);
        } catch(e) {
          return String(params.value);
        }
      }
    } else if (f.type === 'DOMAIN_REFERENCE') {
      colDef.cellRenderer = (params) => {
        if (!params.value) return ''
        const displayVal = getDomainRefDisplayName(f.key, params.value)
        return displayVal ? displayVal : params.value
      }
    } else if (f.type === 'CALCULATED') {
      const opts = JSON.parse(f.options || '{}')
      if (opts.formula) {
        colDef.valueGetter = (params) => {
          if (!params.data || !params.data.data) return ''
          const rawData = params.data.data
          const dataObj = typeof rawData === 'string' ? JSON.parse(rawData) : rawData
          const result = evaluateFormula(opts.formula, dataObj)
          if (result !== null && !isNaN(result)) {
            let formatted = Number(result).toLocaleString('ko-KR')
            if (f.unit) {
              formatted += ` ${f.unit}`
            }
            return formatted;
          }
          return ''
        }
      }
    }
    if (f.type === 'DATE') {
      colDef.valueFormatter = (params) => {
        if (!params.value) return '';
        const date = parseDate(params.value);
        if (!date || isNaN(date.getTime())) return params.value;
        const tz = useCookie('timezone', { default: () => 'Asia/Seoul' }).value;
        const formatted = date.toLocaleString(undefined, { timeZone: tz });
        return formatted.replace(/\s*(GMT|UTC|KST|PST|EST|CET)[-+0-9:]*/gi, '').trim();
      }
    }
    if (['NUMBER', 'INTEGER', 'DECIMAL'].includes(f.type)) {
      colDef.valueFormatter = (params) => {
        if (params.value === null || params.value === undefined || params.value === '') return '';
        const num = Number(params.value);
        if (isNaN(num)) return params.value;
        let formatted = num.toLocaleString('ko-KR');
        if (f.unit) {
          formatted += ` ${f.unit}`;
        }
        return formatted;
      };
    }
    
    if (f.fieldGroup && f.fieldGroup.name) {
      const gName = getTranslatedName(f.fieldGroup.name)
      if (!groupMap[gName]) {
        groupMap[gName] = {
          headerName: gName,
          _sortOrder: f.fieldGroup.sortOrder || 0,
          openByDefault: f.fieldGroup.isDefaultOpen !== false,
          children: []
        }
      }
      
      // 그룹을 접었을 때 그룹 자체가 사라지지 않도록, 첫 번째 컬럼은 항상 보이게 하고 나머지는 열렸을 때만 보이게 설정
      if (groupMap[gName].children.length > 0) {
        colDef.columnGroupShow = 'open';
      }
      
      groupMap[gName].children.push(colDef)
    } else {
      defs.push(colDef)
    }
  })
  
  // Append sorted groups
  Object.values(groupMap)
    .sort((a, b) => a._sortOrder - b._sortOrder)
    .forEach(g => {
      delete g._sortOrder
      defs.push(g)
    })
  
  defs.push({ field: 'createdAt', headerName: 'Created At', sortable: true, width: 180 })
  return defs
}

const columnDefs = ref([])
  
  const activeFilters = ref({})
  const activeFiltersOp = ref({})
  const activeFiltersMax = ref({})
  
  const draftFilters = ref({})
  const draftFiltersOp = ref({})
  const draftFiltersMax = ref({})
  
  const showAdvancedSearch = ref(false)
  const searchableFields = computed(() => nodeFields.value.filter(f => f.isSearchable && !f.isRemoved))
  
  const applyFilters = () => {
    activeFilters.value = { ...draftFilters.value }
    activeFiltersOp.value = { ...draftFiltersOp.value }
    activeFiltersMax.value = { ...draftFiltersMax.value }
    fetchRecords()
  }
  
  const clearFilters = () => {
    draftFilters.value = {}
    draftFiltersOp.value = {}
    draftFiltersMax.value = {}
    activeFilters.value = {}
    activeFiltersOp.value = {}
    activeFiltersMax.value = {}
    fetchRecords()
  }
  
  const removeFilter = (key) => {
    delete draftFilters.value[key]
    delete draftFiltersOp.value[key]
    delete draftFiltersMax.value[key]
    delete activeFilters.value[key]
    delete activeFiltersOp.value[key]
    delete activeFiltersMax.value[key]
    fetchRecords()
  }

  const getFilterFieldLabel = (key) => {
    const f = searchableFields.value.find(f => f.key === key)
    return f ? getTranslatedName(f.name) : key
  }
  
  const formatFilterValue = (key, val) => {
    const op = activeFiltersOp.value[key] || 'EQ'
    const maxVal = activeFiltersMax.value[key]
    if (op === 'BETWEEN') return `${val} ~ ${maxVal || ''}`
    if (op === 'GT') return `> ${val}`
    if (op === 'LT') return `< ${val}`
    if (op === 'GTE') return `>= ${val}`
    if (op === 'LTE') return `<= ${val}`
    return val
  }
  
  const createDatasource = () => {
    return {
      getRows: async (params) => {
        if (!selectedNode.value) {
          params.successCallback([], 0);
          return;
        }
        
        const size = params.endRow - params.startRow;
        const page = Math.floor(params.startRow / size);
        
        try {
          const endpoint = selectedNode.value.isDomain 
            ? `/api/records/domain/${selectedNode.value.id}` 
            : `/api/nodes/${selectedNode.value.id}/records?includeChildren=true`
          
          const searchParams = new URLSearchParams()
          if (endpoint.includes('?')) {
            const parts = endpoint.split('?')
            const qs = new URLSearchParams(parts[1])
            qs.forEach((v, k) => searchParams.append(k, v))
          }
          Object.entries(activeFilters.value).forEach(([k, v]) => {
            if (v !== null && v !== '') {
              searchParams.append('search_' + k, v)
              if (activeFiltersOp.value[k]) {
                searchParams.append('search_op_' + k, activeFiltersOp.value[k])
              }
              if (activeFiltersOp.value[k] === 'BETWEEN' && activeFiltersMax.value[k]) {
                searchParams.append('search_' + k + '_max', activeFiltersMax.value[k])
              }
            }
          })
          
          searchParams.append('page', page);
          searchParams.append('size', size);
          
          const finalEndpoint = endpoint.split('?')[0] + '?' + searchParams.toString();
            
          const pageData = await $fetch(finalEndpoint, {
            headers: { Authorization: `Bearer ${token.value}` }
          });
          
          const rows = pageData.content.map(r => {
            let parsedData = {}
            if (r.data) {
              try {
                parsedData = JSON.parse(r.data)
              } catch(e) {}
            }
            
            const nodeNameMap = r.node?.name || {}
            const nodeName = parseName(nodeNameMap)?.[currentLocale.value] || parseName(nodeNameMap)?.ko || parseName(nodeNameMap)?.en || r.node?.id || 'Unknown'
            
            return { ...r, data: parsedData, nodeName }
          });
          
          params.successCallback(rows, pageData.totalElements);
          
        } catch (e) {
          console.error('Failed to load records:', e);
          params.failCallback();
        }
      }
    };
  };

  const fetchRecords = async () => {
    if (gridApi.value) {
      gridApi.value.setGridOption('datasource', createDatasource());
    }
  }

const onGridReady = (params) => {
  gridApi.value = params.api
  fetchRecords()
}



const formatNumber = (val) => {
  if (val === null || val === undefined || val === '') return val;
  const num = Number(val);
  if (isNaN(num)) return val;
  return num.toLocaleString('ko-KR');
}

const formatViewingValue = (field, val) => {
  if (val === null || val === undefined || val === '') return '-';
  if (field.type === 'DOMAIN_REFERENCE') {
    return getDomainRefDisplayName(field.key, val) || '-';
  }
  if (field.type === 'MULTILINGUAL') {
    try {
      const obj = typeof val === 'string' ? JSON.parse(val) : val;
      return obj[currentLocale.value] || obj.ko || obj.en || JSON.stringify(val);
    } catch(e) {
      return String(val);
    }
  }
  if (field.type === 'FILE') {
    const getFilename = (url) => {
      try {
        if (url.includes('?name=')) return decodeURIComponent(url.split('?name=')[1].split('&')[0]);
        return decodeURIComponent(url.split('/').pop().split('?')[0]) || 'Download';
      } catch(e) { return 'Download'; }
    };
    try {
      const arr = JSON.parse(val);
      if (Array.isArray(arr)) {
        return arr.map(url => `<a href="${url}" target="_blank" style="color: blue; text-decoration: underline;">${getFilename(url)}</a>`).join('<br>');
      }
    } catch(e) {}
    return `<a href="${val}" target="_blank" style="color: blue; text-decoration: underline;">${getFilename(val)}</a>`;
  }
  if (['SELECT', 'MULTI_SELECT'].includes(field.type)) {
    try {
      const opts = JSON.parse(field.options || '[]');
      const mapVal = (v) => {
        const found = opts.find(o => o.key === v);
        if (found && found.label) {
          return found.label[currentLocale.value] || found.label.ko || found.label.en || v;
        }
        return v;
      };
      if (Array.isArray(val)) return val.map(mapVal).join(', ');
      return mapVal(val);
    } catch(e) {}
  }
  if (typeof val === 'object') {
    return val[currentLocale.value] || val.ko || val.en || JSON.stringify(val);
  }
  if (['NUMBER', 'INTEGER', 'DECIMAL', 'CALCULATED'].includes(field.type)) {
    let formatted = formatNumber(val);
    if (field.unit) {
      formatted += ` ${field.unit}`;
    }
    return formatted;
  }
  if (typeof val === 'number') {
    return formatNumber(val);
  }
  return val;
}

const showDetailModal = ref(false)
const showHistoryModal = ref(false)
const showSnapshotModal = ref(false)
const snapshotRecordData = ref({})
const activeSnapshotSectorTab = ref(0)
const { showLoading, hideLoading } = useLoading()
const historyLogs = ref([])
const showDiffModal = ref(false)
const selectedDiffs = ref([])

const historyColumns = [
  { key: 'changedAt', label: '일시', sortable: true },
  { key: 'changedBy', label: '처리자' },
  { key: 'changeType', label: '유형' },
  { key: 'diff', label: '변경 내역' },
  { key: 'actions', label: '동작' }
]

const showApprovalHistoryModal = ref(false)
const selectedApprovalRequest = ref(null)
const selectedReflectionTime = ref(null)

const extractFilename = (url) => {
  if (!url) return 'Download';
  try {
    if (url.includes('?name=')) return decodeURIComponent(url.split('?name=')[1].split('&')[0]);
    return decodeURIComponent(url.split('/').pop().split('?')[0]) || 'Download';
  } catch(e) { return 'Download'; }
};

let draggedItemIndex = null;
let currentArrayRef = null;

const onDragStart = (event, index, arr) => {
  draggedItemIndex = index;
  currentArrayRef = arr;
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move';
  setTimeout(() => {
    if (event.target && event.target.style) event.target.style.opacity = '0.5';
  }, 0);
};

const onDragEnter = (event, index, arr) => {
  if (draggedItemIndex === null || currentArrayRef !== arr) return;
  if (draggedItemIndex === index) return;
  
  const temp = arr[draggedItemIndex];
  arr.splice(draggedItemIndex, 1);
  arr.splice(index, 0, temp);
  draggedItemIndex = index;
};

const onDrop = (event, index, arr) => {
  // onDragEnter already swapped elements, nothing to do here
};

const onDragEnd = (event) => {
  if (event.target && event.target.style) event.target.style.opacity = '1';
  draggedItemIndex = null;
  currentArrayRef = null;
};

const removeFile = (arr, index) => {
  if (!arr || !Array.isArray(arr)) return;
  arr.splice(index, 1);
};

const viewApprovalHistory = async (row) => {
  if (!row.approvalRequestId) return
  selectedApprovalRequest.value = null
  selectedReflectionTime.value = row.changedAt
  showApprovalHistoryModal.value = true
  try {
    const res = await $fetch(`/api/approval-requests/${row.approvalRequestId}`, {
      headers: { Authorization: `Bearer ${token.value}` }
    })
    selectedApprovalRequest.value = res
  } catch (e) {
    console.error('Failed to load approval details', e)
    alert('Failed to load approval details')
    showApprovalHistoryModal.value = false
  }
}

const selectedRecordData = ref({})
const originalRecordData = ref({})
const selectedRecordId = ref(null)
const isEditingRecord = ref(false)
const hasPendingUpdate = ref(false)
const isSnapshotMode = ref(false)

const viewSnapshot = (dataString) => {
  if (!dataString) return
  try {
    snapshotRecordData.value = JSON.parse(dataString)
    activeSnapshotSectorTab.value = activeSectorTab.value
    showSnapshotModal.value = true
  } catch(e) {}
}

const getParsedDiffs = (prev, next) => {
  try {
    let p = {};
    let n = {};
    if (typeof prev === 'string') {
      try {
        const parsed = JSON.parse(prev);
        if (next === 'RECORD_UPDATE') {
          p = parsed.before || {};
          n = parsed.after || {};
        } else {
          n = parsed || {};
        }
      } catch (e) {}
    } else {
      p = prev ? JSON.parse(prev) : {}
      n = next ? JSON.parse(next) : {}
    }
    const diffs = []
    const keys = [...new Set([...Object.keys(p), ...Object.keys(n)])]
    keys.forEach(k => {
      if (JSON.stringify(p[k]) !== JSON.stringify(n[k])) {
        const field = nodeFields.value?.find(f => f.key === k)
        const fName = field ? getTranslatedName(field.name) : k
        
        let valBefore = p[k];
        let valAfter = n[k];
        
        if (field && field.type === 'DOMAIN_REFERENCE') {
          let tDomainId = null;
          try { tDomainId = JSON.parse(field.options || '{}').targetDomainId; } catch(e){}
          if (valBefore) {
            if (typeof valBefore === 'string' && valBefore.length === 36) {
              if (!domainRefDisplayMap.value[valBefore]) fetchDomainRefName(valBefore, tDomainId);
              valBefore = domainRefDisplayMap.value[valBefore] || valBefore;
            }
          }
          if (valAfter) {
            if (typeof valAfter === 'string' && valAfter.length === 36) {
              if (!domainRefDisplayMap.value[valAfter]) fetchDomainRefName(valAfter, tDomainId);
              valAfter = domainRefDisplayMap.value[valAfter] || valAfter;
            }
          }
        }
        
        const formatValue = (val) => {
          if (val === undefined || val === null) return val;
          if (typeof val === 'object') {
            return Object.entries(val).map(([k, v]) => `${k.toUpperCase()}: ${v}`).join(', ');
          }
          if (typeof val === 'number') return val.toLocaleString('ko-KR');
          return val;
        };
        
        valBefore = formatValue(valBefore);
        valAfter = formatValue(valAfter);
        
        diffs.push({
          fieldName: fName,
          before: valBefore,
          after: valAfter
        })
      }
    })
    return diffs
  } catch (e) {
    console.error('getParsedDiffs error:', e)
    return []
  }
}

const viewDiffDetails = (prev, next, isPendingCreation = false) => {
  if (isPendingCreation) {
    selectedDiffs.value = getParsedDiffs(prev, next);
  } else {
    selectedDiffs.value = getParsedDiffs(prev, next);
  }
  showDiffModal.value = true;
}

const parseDate = (dateString) => {
  if (!dateString) return null
  let str = String(dateString).trim()
  if (/^\d+$/.test(str)) {
    return new Date(parseInt(str, 10))
  }
  if (!str.endsWith('Z') && !str.includes('+') && !/[-+]\d{2}:\d{2}$/.test(str)) {
    if (str.includes(' ') && !str.includes('T')) {
      str = str.replace(' ', 'T')
    }
    const serverOffset = useCookie('server_offset', { default: () => '+09:00' }).value
    str += serverOffset
  }
  const d = new Date(str)
  return isNaN(d.getTime()) ? new Date(dateString) : d
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = parseDate(dateString)
  if (!date) return ''
  const tz = useCookie('timezone', { default: () => 'Asia/Seoul' }).value
  const formatted = date.toLocaleString(undefined, { timeZone: tz })
  return formatted.replace(/\s*(GMT|UTC|KST|PST|EST|CET)[-+0-9:]*/gi, '').trim()
}

const openHistory = async () => {
  if (!selectedRecordId.value) return
  showLoading('이력을 불러오는 중입니다...')
  try {
    const res = await $fetch(`/api/records/${selectedRecordId.value}/history`, {
      headers: { Authorization: `Bearer ${token.value}` }
    })
    historyLogs.value = res || []
    
    try {
      const approvals = await $fetch('/api/approval-requests', { headers: { Authorization: `Bearer ${token.value}` } })
      const pending = approvals.find(a => a.targetId === selectedRecordId.value && a.status === 'PENDING')
      if (pending) {
        const fullPending = await $fetch(`/api/approval-requests/${pending.id}`, { headers: { Authorization: `Bearer ${token.value}` } })
        if (!historyLogs.value.some(log => log.id === 'pending-approval-log')) {
          historyLogs.value = [
            {
              id: 'pending-approval-log',
              changedAt: fullPending.createdAt,
              changedBy: fullPending.requesterId,
              changeType: 'PENDING_APPROVAL',
              previousData: null,
              newData: null,
              approvalRequestId: fullPending.id,
              rawRequest: fullPending
            },
            ...historyLogs.value
          ]
        }
      }
    } catch (e) {}
    
    showHistoryModal.value = true
  } catch (e) {
    console.error('Failed to load history', e)
    alert('Failed to load history')
  } finally {
    hideLoading()
  }
}

const onRowDoubleClicked = (params) => {
  if (!params.data || !params.data.data) return
  selectedRecordId.value = params.data.id
  const data = { ...params.data.data }
  nodeFields.value.forEach(f => {
    if (f.type === 'MULTILINGUAL') {
      if (!data[f.key]) {
        data[f.key] = { ko: '', en: '' }
      } else if (typeof data[f.key] === 'string') {
        try {
          data[f.key] = JSON.parse(data[f.key])
        } catch (e) {
          data[f.key] = { ko: data[f.key], en: '' }
        }
      }
    } else if (f.type === 'FILE') {
      if (data[f.key]) {
        try {
          const arr = JSON.parse(data[f.key])
          if (Array.isArray(arr)) {
            data[f.key] = arr.map(url => ({ name: extractFilename(url), url: url }))
          } else if (typeof data[f.key] === 'string') {
            data[f.key] = [{ name: extractFilename(data[f.key]), url: data[f.key] }]
          }
        } catch(e) {
          if (typeof data[f.key] === 'string') {
            data[f.key] = [{ name: extractFilename(data[f.key]), url: data[f.key] }]
          }
        }
      } else {
        data[f.key] = []
      }
    }
  })
  selectedRecordData.value = data
  originalRecordData.value = JSON.parse(JSON.stringify(data))
  activeSectorTab.value = 0
  isEditingRecord.value = false
  isSnapshotMode.value = false
  hasPendingUpdate.value = params.data.status === 'PENDING_APPROVAL' || false
  showDetailModal.value = true
}

const formatDataForSave = (dataObj) => {
  const formatted = { ...dataObj }
  nodeFields.value?.forEach(field => {
    const val = formatted[field.key]
    if (val !== undefined && val !== null && val !== '') {
      if (['NUMBER', 'DECIMAL', 'FLOAT', 'INTEGER'].includes(field.type)) {
        formatted[field.key] = Number(val)
      } else if (field.type === 'BOOLEAN') {
        formatted[field.key] = Boolean(val)
      }
    }
    if (field.type === 'CALCULATED') {
      try {
        const opts = JSON.parse(field.options || '{}')
        if (opts.formula) {
          const result = evaluateFormula(opts.formula, formatted)
          if (result !== null && !isNaN(result)) {
            formatted[field.key] = Number(result)
          }
        }
      } catch (e) {
        console.warn('Failed to compute calculated field', field.key, e)
      }
    }
  })
  return formatted
}

const saveEditedRecord = async () => {
  try {
    let reqId = currentUser.value?.uuid || '123e4567-e89b-12d3-a456-426614174000'
    const dataToSave = { ...selectedRecordData.value }
    for (const field of nodeFields.value) {
      if (field.type === 'FILE') {
        let files = selectedRecordData.value[field.key]
        if (!files) files = []
        if (!Array.isArray(files)) files = [files]
        
        const uploadedUrls = []
        for (const file of files) {
          if (file instanceof File) {
            const fd = new FormData()
            fd.append('file', file)
            const res = await $fetch('/api/files/upload', {
              method: 'POST',
              headers: { Authorization: `Bearer ${token.value}` },
              body: fd
            })
            uploadedUrls.push(res.url)
          } else if (typeof file === 'string') {
            uploadedUrls.push(file)
          } else if (file && file.url) {
            uploadedUrls.push(file.url)
          }
        }
        if (uploadedUrls.length > 0) {
          dataToSave[field.key] = JSON.stringify(uploadedUrls)
        } else {
          dataToSave[field.key] = "[]"
        }
      } else {
        dataToSave[field.key] = selectedRecordData.value[field.key]
      }
    }
    const payload = { requesterId: reqId, data: JSON.stringify(formatDataForSave(dataToSave)), comment: draftCommentText.value }
    await $fetch(`/api/records/${selectedRecordId.value}/update-request`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token.value}` },
      body: payload
    })
    isEditingRecord.value = false
    showDetailModal.value = false
    alert('Record update request submitted successfully for approval.')
    await fetchRecords()
  } catch (e) {
    console.error('Failed to update record:', e)
    const errorMsg = typeof e.response?._data === 'string' ? e.response._data : (e.response?._data?.message || e.message || 'Failed to update record.')
    alert('Failed to submit update request: ' + errorMsg)
  }
}

const requestDeleteRecord = async () => {
  if (!confirm('Are you sure you want to request deletion for this record?')) return
  try {
    let reqId = currentUser.value?.uuid || '123e4567-e89b-12d3-a456-426614174000'
    const payload = { requesterId: reqId, data: "{}" }
    await $fetch(`/api/records/${selectedRecordId.value}/delete-request`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token.value}` },
      body: payload
    })
    showDetailModal.value = false
    alert('Record deletion request submitted successfully for approval.')
    await fetchRecords()
  } catch (e) {
    console.error('Failed to request deletion:', e)
    const errorMsg = typeof e.response?._data === 'string' ? e.response._data : (e.response?._data?.message || e.message || 'Failed to request deletion.')
    alert('Failed to submit deletion request: ' + errorMsg)
  }
}

const defaultColDef = {
  minWidth: 100,
  resizable: true,
  cellDataType: false
}

const evaluateFormula = (formula, data) => {
  try {
    const replaced = formula.replace(/\${([^}]+)}/g, (_, key) => {
      const val = data[key]
      return val != null && val !== '' ? val : '0'
    })
    const ROUND = (val, dec=0) => Number(Math.round(val+'e'+dec)+'e-'+dec);
    const fn = new Function('ROUND', 'ABS', 'CEIL', 'FLOOR', `return ${replaced};`)
    return fn(ROUND, Math.abs, Math.ceil, Math.floor)
  } catch (e) {
    console.warn('Formula evaluation failed', e)
    return null
  }
}

let isCalculating = false;

const handleCalculatedFields = (newData) => {
  if (isCalculating) return;
  
  const calculatedFields = nodeFields.value.filter(f => f.type === 'CALCULATED')
  if (calculatedFields.length === 0) return

  isCalculating = true;
  let changed = false;
  
  for (let pass = 0; pass < 3; pass++) {
    let passChanged = false;
    for (const field of calculatedFields) {
      try {
        const opts = JSON.parse(field.options || '{}')
        if (opts.formula) {
          const result = evaluateFormula(opts.formula, newData)
          if (result !== null && !isNaN(result) && String(newData[field.key]) !== String(result)) {
            newData[field.key] = result
            passChanged = true
            changed = true
          }
        }
      } catch(e) {}
    }
    if (!passChanged) break;
  }
  isCalculating = false;
}

watch(recordFormData, handleCalculatedFields, { deep: true })
watch(selectedRecordData, handleCalculatedFields, { deep: true })

const openCreateModal = () => {
  const initialData = {}
  nodeFields.value.forEach(f => {
    if (f.type === 'MULTILINGUAL') initialData[f.key] = { ko: '', en: '' }
  })
  recordFormData.value = initialData
  activeSectorTab.value = 0
  showCreateModal.value = true
}

const showDraftCommentModal = ref(false)
const draftCommentText = ref('')
const pendingSaveAction = ref(null)

const handleExcelUploaded = () => {
  showExcelUploader.value = false;
  fetchRecords();
  alert('Bulk upload completed! Requests are now in PENDING status.');
}

const promptDraftComment = (action) => {
  if (action === 'UPDATE') {
    const orig = JSON.stringify(formatDataForSave(originalRecordData.value))
    const curr = JSON.stringify(formatDataForSave(selectedRecordData.value))
    if (orig === curr) {
      alert('변경된 데이터가 없습니다.')
      return
    }
  }
  pendingSaveAction.value = action
  draftCommentText.value = ''
  showDraftCommentModal.value = true
}

const executePendingSave = async () => {
  if (pendingSaveAction.value === 'CREATE') {
    await saveRecord()
  } else if (pendingSaveAction.value === 'UPDATE') {
    await saveEditedRecord()
  }
  showDraftCommentModal.value = false
}

const saveRecord = async () => {
  if (!selectedNode.value) return
  try {
    let reqId = currentUser.value?.uuid
    if (!reqId || reqId === 'test-admin-uuid') {
       reqId = '123e4567-e89b-12d3-a456-426614174000' 
    }

    const dataToSave = { ...recordFormData.value }
    for (const field of nodeFields.value) {
      if (field.type === 'FILE' && recordFormData.value[field.key]) {
        let files = recordFormData.value[field.key]
        if (!Array.isArray(files)) {
          files = [files]
        }
        
        const uploadedUrls = []
        for (const file of files) {
          if (file instanceof File) {
            const fd = new FormData()
            fd.append('file', file)
            const res = await $fetch('/api/files/upload', {
              method: 'POST',
              headers: { Authorization: `Bearer ${token.value}` },
              body: fd
            })
            uploadedUrls.push(res.url)
          } else if (typeof file === 'string') {
            uploadedUrls.push(file)
          }
        }
        
        if (uploadedUrls.length > 0) {
          dataToSave[field.key] = isMultiple(field) ? JSON.stringify(uploadedUrls) : uploadedUrls[0]
        } else {
          dataToSave[field.key] = null
        }
      } else {
        dataToSave[field.key] = recordFormData.value[field.key]
      }
    }

    const formattedData = formatDataForSave(dataToSave)

    const payload = {
      data: JSON.stringify(formattedData),
      requesterId: reqId,
      comment: draftCommentText.value
    }
    
    await $fetch(`/api/nodes/${selectedNode.value.id}/records`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token.value}` },
      body: payload
    })
    
    showCreateModal.value = false
    await fetchRecords()
  } catch (error) {
    const errorMsg = error.response?._data?.message || error.message || String(error)
    alert(`Data Quality / Workflow Error:\n\n${errorMsg}`)
    console.error('Full error:', error, error.response?._data)
  }
}
</script>

<style scoped>
.records-layout {
  display: flex;
  height: 100%;
  width: 100%;
  min-height: 0;
}
.records-tree-column {
  width: 300px;
  min-width: 300px;
  border-right: 1px solid #ddd;
  display: flex;
  flex-direction: column;
}
.records-detail-column {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 0 0 0 1rem;
  box-sizing: border-box;
}
.records-grid-wrapper {
  flex: 1;
  width: 100%;
  min-height: 0;
}

@media (max-width: 768px) {
  .records-layout {
    flex-direction: column;
  }
  .records-tree-column {
    width: 100%;
    min-width: 100%;
    border-right: none;
    border-bottom: 1px solid #ddd;
    max-height: 250px;
  }
  .records-detail-column {
    padding: 0.25rem 0;
  }
  .records-grid-wrapper {
    height: 400px;
  }
}
</style>

<style scoped>
.mb-4 { margin-bottom: 1rem; }
.mt-2 { margin-top: 0.5rem; }
.w-full { width: 100%; }

:deep(.va-tree) {
  overflow-x: hidden;
}

.file-upload-wrapper :deep(.va-file-upload) {
  width: 100% !important;
  max-width: 100% !important;
}
.file-upload-wrapper :deep(.va-file-upload-dropzone) {
  box-sizing: border-box !important;
  padding: 0.5rem 1rem !important;
  min-height: 60px !important;
}
.file-upload-wrapper :deep(.va-file-upload-dropzone__content) {
  width: 100% !important;
  box-sizing: border-box !important;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 1rem;
}
.file-upload-wrapper :deep(.va-file-upload-list) {
  display: none !important;
}
.custom-file-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 0.5rem;
}
.custom-file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 1rem;
  background-color: var(--va-background-element);
  border-radius: 0.5rem;
  font-size: 0.9rem;
}
.custom-file-info {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.custom-file-actions {
  display: flex;
  gap: 0.25rem;
  align-items: center;
}
.flip-list-move {
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.5, 1);
}

.slim-multilingual-input :deep(.va-input-wrapper__field) {
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  align-items: center;
  min-height: 28px !important;
  height: 28px !important;
}
.slim-multilingual-input :deep(.va-input-wrapper__container) {
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  margin-top: 0 !important;
  margin-bottom: 0 !important;
  min-height: 28px !important;
  height: 28px !important;
}
.slim-multilingual-input :deep(input) {
  height: 100% !important;
  line-height: 28px !important;
}

</style>
