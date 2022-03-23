package com.auth0.flickr2.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.auth0.flickr2.IntegrationTest;
import com.auth0.flickr2.domain.Photo;
import com.auth0.flickr2.repository.EntityManager;
import com.auth0.flickr2.repository.PhotoRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Integration tests for the {@link PhotoResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class PhotoResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final byte[] DEFAULT_IMAGE = TestUtil.createByteArray(1, "0");
    private static final byte[] UPDATED_IMAGE = TestUtil.createByteArray(1, "1");
    private static final String DEFAULT_IMAGE_CONTENT_TYPE = "image/jpg";
    private static final String UPDATED_IMAGE_CONTENT_TYPE = "image/png";

    private static final Integer DEFAULT_HEIGHT = 1;
    private static final Integer UPDATED_HEIGHT = 2;

    private static final Integer DEFAULT_WIDTH = 1;
    private static final Integer UPDATED_WIDTH = 2;

    private static final Instant DEFAULT_TAKEN = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_TAKEN = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPLOADED = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPLOADED = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/photos";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private PhotoRepository photoRepository;

    @Mock
    private PhotoRepository photoRepositoryMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Photo photo;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Photo createEntity(EntityManager em) {
        Photo photo = new Photo()
            .title(DEFAULT_TITLE)
            .description(DEFAULT_DESCRIPTION)
            .image(DEFAULT_IMAGE)
            .imageContentType(DEFAULT_IMAGE_CONTENT_TYPE)
            .height(DEFAULT_HEIGHT)
            .width(DEFAULT_WIDTH)
            .taken(DEFAULT_TAKEN)
            .uploaded(DEFAULT_UPLOADED);
        return photo;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Photo createUpdatedEntity(EntityManager em) {
        Photo photo = new Photo()
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .image(UPDATED_IMAGE)
            .imageContentType(UPDATED_IMAGE_CONTENT_TYPE)
            .height(UPDATED_HEIGHT)
            .width(UPDATED_WIDTH)
            .taken(UPDATED_TAKEN)
            .uploaded(UPDATED_UPLOADED);
        return photo;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll("rel_photo__tag").block();
            em.deleteAll(Photo.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @AfterEach
    public void cleanup() {
        deleteEntities(em);
    }

    @BeforeEach
    public void setupCsrf() {
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @BeforeEach
    public void initTest() {
        deleteEntities(em);
        photo = createEntity(em);
    }

    @Test
    void createPhoto() throws Exception {
        int databaseSizeBeforeCreate = photoRepository.findAll().collectList().block().size();
        // Create the Photo
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(photo))
            .exchange()
            .expectStatus()
            .isCreated();

        // Validate the Photo in the database
        List<Photo> photoList = photoRepository.findAll().collectList().block();
        assertThat(photoList).hasSize(databaseSizeBeforeCreate + 1);
        Photo testPhoto = photoList.get(photoList.size() - 1);
        assertThat(testPhoto.getTitle()).isEqualTo(DEFAULT_TITLE);
        assertThat(testPhoto.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testPhoto.getImage()).isEqualTo(DEFAULT_IMAGE);
        assertThat(testPhoto.getImageContentType()).isEqualTo(DEFAULT_IMAGE_CONTENT_TYPE);
        assertThat(testPhoto.getHeight()).isEqualTo(DEFAULT_HEIGHT);
        assertThat(testPhoto.getWidth()).isEqualTo(DEFAULT_WIDTH);
        assertThat(testPhoto.getTaken()).isEqualTo(DEFAULT_TAKEN);
        assertThat(testPhoto.getUploaded()).isEqualTo(DEFAULT_UPLOADED);
    }

    @Test
    void createPhotoWithExistingId() throws Exception {
        // Create the Photo with an existing ID
        photo.setId(1L);

        int databaseSizeBeforeCreate = photoRepository.findAll().collectList().block().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(photo))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Photo in the database
        List<Photo> photoList = photoRepository.findAll().collectList().block();
        assertThat(photoList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    void checkTitleIsRequired() throws Exception {
        int databaseSizeBeforeTest = photoRepository.findAll().collectList().block().size();
        // set the field null
        photo.setTitle(null);

        // Create the Photo, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(photo))
            .exchange()
            .expectStatus()
            .isBadRequest();

        List<Photo> photoList = photoRepository.findAll().collectList().block();
        assertThat(photoList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    void getAllPhotos() {
        // Initialize the database
        photoRepository.save(photo).block();

        // Get all the photoList
        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(photo.getId().intValue()))
            .jsonPath("$.[*].title")
            .value(hasItem(DEFAULT_TITLE))
            .jsonPath("$.[*].description")
            .value(hasItem(DEFAULT_DESCRIPTION.toString()))
            .jsonPath("$.[*].imageContentType")
            .value(hasItem(DEFAULT_IMAGE_CONTENT_TYPE))
            .jsonPath("$.[*].image")
            .value(hasItem(Base64Utils.encodeToString(DEFAULT_IMAGE)))
            .jsonPath("$.[*].height")
            .value(hasItem(DEFAULT_HEIGHT))
            .jsonPath("$.[*].width")
            .value(hasItem(DEFAULT_WIDTH))
            .jsonPath("$.[*].taken")
            .value(hasItem(DEFAULT_TAKEN.toString()))
            .jsonPath("$.[*].uploaded")
            .value(hasItem(DEFAULT_UPLOADED.toString()));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPhotosWithEagerRelationshipsIsEnabled() {
        when(photoRepositoryMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(photoRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPhotosWithEagerRelationshipsIsNotEnabled() {
        when(photoRepositoryMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(photoRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getPhoto() {
        // Initialize the database
        photoRepository.save(photo).block();

        // Get the photo
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, photo.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(photo.getId().intValue()))
            .jsonPath("$.title")
            .value(is(DEFAULT_TITLE))
            .jsonPath("$.description")
            .value(is(DEFAULT_DESCRIPTION.toString()))
            .jsonPath("$.imageContentType")
            .value(is(DEFAULT_IMAGE_CONTENT_TYPE))
            .jsonPath("$.image")
            .value(is(Base64Utils.encodeToString(DEFAULT_IMAGE)))
            .jsonPath("$.height")
            .value(is(DEFAULT_HEIGHT))
            .jsonPath("$.width")
            .value(is(DEFAULT_WIDTH))
            .jsonPath("$.taken")
            .value(is(DEFAULT_TAKEN.toString()))
            .jsonPath("$.uploaded")
            .value(is(DEFAULT_UPLOADED.toString()));
    }

    @Test
    void getNonExistingPhoto() {
        // Get the photo
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putNewPhoto() throws Exception {
        // Initialize the database
        photoRepository.save(photo).block();

        int databaseSizeBeforeUpdate = photoRepository.findAll().collectList().block().size();

        // Update the photo
        Photo updatedPhoto = photoRepository.findById(photo.getId()).block();
        updatedPhoto
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .image(UPDATED_IMAGE)
            .imageContentType(UPDATED_IMAGE_CONTENT_TYPE)
            .height(UPDATED_HEIGHT)
            .width(UPDATED_WIDTH)
            .taken(UPDATED_TAKEN)
            .uploaded(UPDATED_UPLOADED);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedPhoto.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(updatedPhoto))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Photo in the database
        List<Photo> photoList = photoRepository.findAll().collectList().block();
        assertThat(photoList).hasSize(databaseSizeBeforeUpdate);
        Photo testPhoto = photoList.get(photoList.size() - 1);
        assertThat(testPhoto.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testPhoto.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testPhoto.getImage()).isEqualTo(UPDATED_IMAGE);
        assertThat(testPhoto.getImageContentType()).isEqualTo(UPDATED_IMAGE_CONTENT_TYPE);
        assertThat(testPhoto.getHeight()).isEqualTo(UPDATED_HEIGHT);
        assertThat(testPhoto.getWidth()).isEqualTo(UPDATED_WIDTH);
        assertThat(testPhoto.getTaken()).isEqualTo(UPDATED_TAKEN);
        assertThat(testPhoto.getUploaded()).isEqualTo(UPDATED_UPLOADED);
    }

    @Test
    void putNonExistingPhoto() throws Exception {
        int databaseSizeBeforeUpdate = photoRepository.findAll().collectList().block().size();
        photo.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, photo.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(photo))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Photo in the database
        List<Photo> photoList = photoRepository.findAll().collectList().block();
        assertThat(photoList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchPhoto() throws Exception {
        int databaseSizeBeforeUpdate = photoRepository.findAll().collectList().block().size();
        photo.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(photo))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Photo in the database
        List<Photo> photoList = photoRepository.findAll().collectList().block();
        assertThat(photoList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamPhoto() throws Exception {
        int databaseSizeBeforeUpdate = photoRepository.findAll().collectList().block().size();
        photo.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(photo))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Photo in the database
        List<Photo> photoList = photoRepository.findAll().collectList().block();
        assertThat(photoList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdatePhotoWithPatch() throws Exception {
        // Initialize the database
        photoRepository.save(photo).block();

        int databaseSizeBeforeUpdate = photoRepository.findAll().collectList().block().size();

        // Update the photo using partial update
        Photo partialUpdatedPhoto = new Photo();
        partialUpdatedPhoto.setId(photo.getId());

        partialUpdatedPhoto
            .title(UPDATED_TITLE)
            .image(UPDATED_IMAGE)
            .imageContentType(UPDATED_IMAGE_CONTENT_TYPE)
            .height(UPDATED_HEIGHT)
            .taken(UPDATED_TAKEN);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedPhoto.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedPhoto))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Photo in the database
        List<Photo> photoList = photoRepository.findAll().collectList().block();
        assertThat(photoList).hasSize(databaseSizeBeforeUpdate);
        Photo testPhoto = photoList.get(photoList.size() - 1);
        assertThat(testPhoto.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testPhoto.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testPhoto.getImage()).isEqualTo(UPDATED_IMAGE);
        assertThat(testPhoto.getImageContentType()).isEqualTo(UPDATED_IMAGE_CONTENT_TYPE);
        assertThat(testPhoto.getHeight()).isEqualTo(UPDATED_HEIGHT);
        assertThat(testPhoto.getWidth()).isEqualTo(DEFAULT_WIDTH);
        assertThat(testPhoto.getTaken()).isEqualTo(UPDATED_TAKEN);
        assertThat(testPhoto.getUploaded()).isEqualTo(DEFAULT_UPLOADED);
    }

    @Test
    void fullUpdatePhotoWithPatch() throws Exception {
        // Initialize the database
        photoRepository.save(photo).block();

        int databaseSizeBeforeUpdate = photoRepository.findAll().collectList().block().size();

        // Update the photo using partial update
        Photo partialUpdatedPhoto = new Photo();
        partialUpdatedPhoto.setId(photo.getId());

        partialUpdatedPhoto
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .image(UPDATED_IMAGE)
            .imageContentType(UPDATED_IMAGE_CONTENT_TYPE)
            .height(UPDATED_HEIGHT)
            .width(UPDATED_WIDTH)
            .taken(UPDATED_TAKEN)
            .uploaded(UPDATED_UPLOADED);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedPhoto.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedPhoto))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Photo in the database
        List<Photo> photoList = photoRepository.findAll().collectList().block();
        assertThat(photoList).hasSize(databaseSizeBeforeUpdate);
        Photo testPhoto = photoList.get(photoList.size() - 1);
        assertThat(testPhoto.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testPhoto.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testPhoto.getImage()).isEqualTo(UPDATED_IMAGE);
        assertThat(testPhoto.getImageContentType()).isEqualTo(UPDATED_IMAGE_CONTENT_TYPE);
        assertThat(testPhoto.getHeight()).isEqualTo(UPDATED_HEIGHT);
        assertThat(testPhoto.getWidth()).isEqualTo(UPDATED_WIDTH);
        assertThat(testPhoto.getTaken()).isEqualTo(UPDATED_TAKEN);
        assertThat(testPhoto.getUploaded()).isEqualTo(UPDATED_UPLOADED);
    }

    @Test
    void patchNonExistingPhoto() throws Exception {
        int databaseSizeBeforeUpdate = photoRepository.findAll().collectList().block().size();
        photo.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, photo.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(photo))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Photo in the database
        List<Photo> photoList = photoRepository.findAll().collectList().block();
        assertThat(photoList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchPhoto() throws Exception {
        int databaseSizeBeforeUpdate = photoRepository.findAll().collectList().block().size();
        photo.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(photo))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Photo in the database
        List<Photo> photoList = photoRepository.findAll().collectList().block();
        assertThat(photoList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamPhoto() throws Exception {
        int databaseSizeBeforeUpdate = photoRepository.findAll().collectList().block().size();
        photo.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(photo))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Photo in the database
        List<Photo> photoList = photoRepository.findAll().collectList().block();
        assertThat(photoList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void deletePhoto() {
        // Initialize the database
        photoRepository.save(photo).block();

        int databaseSizeBeforeDelete = photoRepository.findAll().collectList().block().size();

        // Delete the photo
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, photo.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        List<Photo> photoList = photoRepository.findAll().collectList().block();
        assertThat(photoList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
