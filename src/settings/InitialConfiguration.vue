<script setup lang="ts">
import { invoke } from '@tauri-apps/api/core';
import { open } from '@tauri-apps/plugin-dialog';
import { onMounted, ref } from 'vue';

const apiId = ref('');
const apiHash = ref('');
const applicationFolder = ref('');
const pgpPassphrase = ref('');
const pgpKeyFingerprint = ref('');
const pgpError = ref('');
const encryptDatabase = ref(true);
const validationErrors = ref({} as Record<string, string>);
const initError = ref('');
const loadingKey = ref(false);

onMounted(async () => {
    applicationFolder.value = await invoke<string>("get_default_folder");
});

async function generatePgpKey() {
    pgpKeyFingerprint.value = '';
    pgpError.value = '';
    try {
        loadingKey.value = true;
        const fingerprint = await invoke<string>("generate_pgp_key", { passphrase: pgpPassphrase.value });
        pgpKeyFingerprint.value = fingerprint;
    } catch (err) {
        pgpError.value = `Error generating PGP key: ${(err as string)}`;
    } finally {
        loadingKey.value = false;
    }
}

async function importPgpKey() {
    const file = await open({ multiple: false, directory: false, });
    if (file === null) {
        return;
    }
    pgpKeyFingerprint.value = '';
    pgpError.value = '';
    try {
        loadingKey.value = true;
        const fingerprint = await invoke<string>("import_pgp_key", {
            keyPath: file, passphrase: pgpPassphrase.value
        });
        pgpKeyFingerprint.value = fingerprint;
    } catch (err) {
        pgpError.value = `Error importing PGP key: ${(err as string)}`;
    } finally {
        loadingKey.value = false;
    }
}

function resetKey() {
    pgpKeyFingerprint.value = '';
}

async function next() {
    initError.value = '';
    const valid = validateFields();
    if (!valid) {
        return;
    }
    if (!pgpKeyFingerprint.value) {
        initError.value = 'Please generate or import a PGP key';
        return;
    }
    try {
        await invoke<string>("save_initial_config", {
            apiId: apiId.value,
            apiHash: apiHash.value,
            folder: applicationFolder.value,
            pgpPassphrase: pgpPassphrase.value,
            encryptDb: encryptDatabase.value,
        });
    } catch (err) {
        initError.value = err as string;
    }
}

function validateFields() {
    validationErrors.value = {};
    if (!apiId.value) {
        validationErrors.value['apiId'] = 'Field is required';
    }
    if (!apiHash.value) {
        validationErrors.value['apiHash'] = 'Field is required';
    }
    if (!applicationFolder.value) {
        validationErrors.value['applicationFolder'] = 'Field is required';
    }
    return Object.keys(validationErrors.value).length === 0;
}
</script>

<template>
    <form @submit.prevent="next" class="m-5">
        <div class="field mb-0">
            <label class="label mb-0" for="api-id">API ID</label>
            <div class="control">
                <input class="input" type="text" id="api-id" v-model="apiId"
                    :class="{ 'is-danger': validationErrors['apiId'] }">
            </div>
        </div>
        <span class="has-text-danger" v-if="validationErrors['apiId']">
            {{ validationErrors['apiId'] }}
        </span>
        <div class="field mb-0 mt-3">
            <label class="label mb-0" for="api-hash">API Hash</label>
            <div class="control">
                <input class="input" type="text" id="api-hash" v-model="apiHash"
                    :class="{ 'is-danger': validationErrors['apiHash'] }">
            </div>
        </div>
        <span class="has-text-danger" v-if="validationErrors['apiHash']">
            {{ validationErrors['apiHash'] }}
        </span>
        <p class="mt-1"><a href="https://my.telegram.org" target="_blank">Obtain API credentials</a></p>
        <div class="field mb-0 mt-2">
            <label class="label mb-0" for="application-folder">Application folder</label>
            <div class="control">
                <input class="input" type="text" id="application-folder" v-model="applicationFolder"
                    :class="{ 'is-danger': validationErrors['applicationFolder'] }">
            </div>
        </div>
        <span class="has-text-danger" v-if="validationErrors['applicationFolder']">
            {{ validationErrors['applicationFolder'] }}
        </span>
        <div class="field mt-3">
            <label class="label mb-0" for="passphrase">PGP key passphrase</label>
            <div class="control">
                <input class="input" type="passphrase" id="passphrase" v-model="pgpPassphrase" @input="resetKey"
                    @change="resetKey">
            </div>
        </div>
        <div class="field is-grouped">
            <div class="control">
                <button type="button" class="button is-primary is-dark mr-2" @click="generatePgpKey"
                    :disabled="loadingKey">
                    Generate PGP key
                </button>
                <button type="button" class="button is-primary" @click="importPgpKey" :disabled="loadingKey">
                    Select PGP key
                </button>
            </div>
        </div>
        <div class="message is-success" v-if="pgpKeyFingerprint">
            <div class="message-body">
                Key fingerprint: <code>{{ pgpKeyFingerprint }}</code>
            </div>
        </div>
        <div class="message is-danger" v-if="pgpError">
            <div class="message-body">
                {{ pgpError }}
            </div>
        </div>
        <div class="field mb-0">
            <label class="checkbox">
                <input type="checkbox" v-bind:checked="encryptDatabase" />
                Encrypt Telegram database
            </label>
        </div>
        <p class="has-text-primary"><i>A random password encrypted with your PGP key will be used</i></p>
        <div class="message is-danger mt-3" v-if="initError">
            <div class="message-body">
                {{ initError }}
            </div>
        </div>
        <div class="field is-grouped mt-3">
            <div class="control">
                <button class="button is-link" type="submit">Next</button>
            </div>
        </div>
    </form>
</template>