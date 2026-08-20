package com.isdemir.isdemirdb.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.isdemir.isdemirdb.exception.InvalidDataException;
import com.isdemir.isdemirdb.exception.RecordNotFoundException;

import lombok.extern.slf4j.Slf4j;

// The Supabase Storage bucket the heat photos live in: the mobile app uploads there, the
// panel now does too. The bucket is private, so the browser can neither read nor write it
// directly - the backend does both with the service key, behind the usual JWT.
//
// The URL and the key come from configuration (application-supabase.properties, gitignored)
// - they are never in the repository.
@Service
@Slf4j
public class SupabaseStorage {

    private final RestClient client;
    private final String bucketUrl;

    public SupabaseStorage(@Value("${supabase.storage-url:}") String bucketUrl,
            @Value("${supabase.service-key:}") String serviceKey) {
        this.bucketUrl = bucketUrl;
        this.client = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                .build();
    }

    // The bytes of one file. Anything that goes wrong on the storage side (missing object,
    // bad key, no configuration) is a 404 for the caller: the photo cannot be shown.
    public byte[] download(String path) {
        if (path == null || path.isBlank()) {
            throw new RecordNotFoundException("Fotoğrafın dosya yolu yok");
        }
        if (bucketUrl.isBlank()) {
            throw new RecordNotFoundException("Fotoğraf deposu ayarlı değil (supabase.storage-url)");
        }
        try {
            byte[] bytes = client.get().uri(bucketUrl + path).retrieve().body(byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new RecordNotFoundException("Fotoğraf dosyası boş: " + path);
            }
            return bytes;
        } catch (RestClientException ex) {
            log.warn("Supabase Storage'dan indirilemedi ({}): {}", path, ex.getMessage());
            throw new RecordNotFoundException("Fotoğraf dosyası alınamadı");
        }
    }

    // Puts a new file into the bucket. The path is built by the caller (dokum/<id>/<uuid>.jpg),
    // so an upload can never land on an existing object.
    public void upload(String path, byte[] bytes, String contentType) {
        if (bucketUrl.isBlank()) {
            throw new InvalidDataException("Fotoğraf deposu ayarlı değil (supabase.storage-url)");
        }
        try {
            client.post()
                    .uri(bucketUrl + path)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Supabase Storage'a yuklenemedi ({}): {}", path, ex.getMessage());
            throw new InvalidDataException("Fotoğraf depoya yüklenemedi");
        }
    }

    // Removes a file from the bucket. A photo the user deleted has to disappear from the
    // screen even when the storage says no (the object is already gone, a temporary error):
    // the row is dropped either way, at worst an unreferenced file stays behind - a line in
    // the log is enough for that.
    public void delete(String path) {
        if (path == null || path.isBlank() || bucketUrl.isBlank()) {
            return;
        }
        try {
            client.delete().uri(bucketUrl + path).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Supabase Storage'dan silinemedi ({}): {}", path, ex.getMessage());
        }
    }
}
