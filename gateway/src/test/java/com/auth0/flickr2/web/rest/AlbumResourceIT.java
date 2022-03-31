package com.auth0.flickr2.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.auth0.flickr2.IntegrationTest;
import com.auth0.flickr2.domain.Album;
import com.auth0.flickr2.repository.AlbumRepository;
import com.auth0.flickr2.repository.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * Integration tests for the {@link AlbumResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class AlbumResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/albums";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private AlbumRepository albumRepository;

    @Mock
    private AlbumRepository albumRepositoryMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Album album;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Album createEntity(EntityManager em) {
        Album album = new Album().title(DEFAULT_TITLE).description(DEFAULT_DESCRIPTION).created(DEFAULT_CREATED);
        return album;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Album createUpdatedEntity(EntityManager em) {
        Album album = new Album().title(UPDATED_TITLE).description(UPDATED_DESCRIPTION).created(UPDATED_CREATED);
        return album;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Album.class).block();
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
        album = createEntity(em);
    }

    @Test
    void createAlbum() throws Exception {
        int databaseSizeBeforeCreate = albumRepository.findAll().collectList().block().size();
        // Create the Album
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(album))
            .exchange()
            .expectStatus()
            .isCreated();

        // Validate the Album in the database
        List<Album> albumList = albumRepository.findAll().collectList().block();
        assertThat(albumList).hasSize(databaseSizeBeforeCreate + 1);
        Album testAlbum = albumList.get(albumList.size() - 1);
        assertThat(testAlbum.getTitle()).isEqualTo(DEFAULT_TITLE);
        assertThat(testAlbum.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testAlbum.getCreated()).isEqualTo(DEFAULT_CREATED);
    }

    @Test
    void createAlbumWithExistingId() throws Exception {
        // Create the Album with an existing ID
        album.setId(1L);

        int databaseSizeBeforeCreate = albumRepository.findAll().collectList().block().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(album))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Album in the database
        List<Album> albumList = albumRepository.findAll().collectList().block();
        assertThat(albumList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    void checkTitleIsRequired() throws Exception {
        int databaseSizeBeforeTest = albumRepository.findAll().collectList().block().size();
        // set the field null
        album.setTitle(null);

        // Create the Album, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(album))
            .exchange()
            .expectStatus()
            .isBadRequest();

        List<Album> albumList = albumRepository.findAll().collectList().block();
        assertThat(albumList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    void getAllAlbums() {
        // Initialize the database
        albumRepository.save(album).block();

        // Get all the albumList
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
            .value(hasItem(album.getId().intValue()))
            .jsonPath("$.[*].title")
            .value(hasItem(DEFAULT_TITLE))
            .jsonPath("$.[*].description")
            .value(hasItem(DEFAULT_DESCRIPTION.toString()))
            .jsonPath("$.[*].created")
            .value(hasItem(DEFAULT_CREATED.toString()));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAlbumsWithEagerRelationshipsIsEnabled() {
        when(albumRepositoryMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(albumRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAlbumsWithEagerRelationshipsIsNotEnabled() {
        when(albumRepositoryMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(albumRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getAlbum() {
        // Initialize the database
        albumRepository.save(album).block();

        // Get the album
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, album.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(album.getId().intValue()))
            .jsonPath("$.title")
            .value(is(DEFAULT_TITLE))
            .jsonPath("$.description")
            .value(is(DEFAULT_DESCRIPTION.toString()))
            .jsonPath("$.created")
            .value(is(DEFAULT_CREATED.toString()));
    }

    @Test
    void getNonExistingAlbum() {
        // Get the album
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putNewAlbum() throws Exception {
        // Initialize the database
        albumRepository.save(album).block();

        int databaseSizeBeforeUpdate = albumRepository.findAll().collectList().block().size();

        // Update the album
        Album updatedAlbum = albumRepository.findById(album.getId()).block();
        updatedAlbum.title(UPDATED_TITLE).description(UPDATED_DESCRIPTION).created(UPDATED_CREATED);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedAlbum.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(updatedAlbum))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Album in the database
        List<Album> albumList = albumRepository.findAll().collectList().block();
        assertThat(albumList).hasSize(databaseSizeBeforeUpdate);
        Album testAlbum = albumList.get(albumList.size() - 1);
        assertThat(testAlbum.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testAlbum.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testAlbum.getCreated()).isEqualTo(UPDATED_CREATED);
    }

    @Test
    void putNonExistingAlbum() throws Exception {
        int databaseSizeBeforeUpdate = albumRepository.findAll().collectList().block().size();
        album.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, album.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(album))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Album in the database
        List<Album> albumList = albumRepository.findAll().collectList().block();
        assertThat(albumList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchAlbum() throws Exception {
        int databaseSizeBeforeUpdate = albumRepository.findAll().collectList().block().size();
        album.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(album))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Album in the database
        List<Album> albumList = albumRepository.findAll().collectList().block();
        assertThat(albumList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamAlbum() throws Exception {
        int databaseSizeBeforeUpdate = albumRepository.findAll().collectList().block().size();
        album.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(album))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Album in the database
        List<Album> albumList = albumRepository.findAll().collectList().block();
        assertThat(albumList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateAlbumWithPatch() throws Exception {
        // Initialize the database
        albumRepository.save(album).block();

        int databaseSizeBeforeUpdate = albumRepository.findAll().collectList().block().size();

        // Update the album using partial update
        Album partialUpdatedAlbum = new Album();
        partialUpdatedAlbum.setId(album.getId());

        partialUpdatedAlbum.description(UPDATED_DESCRIPTION).created(UPDATED_CREATED);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedAlbum.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedAlbum))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Album in the database
        List<Album> albumList = albumRepository.findAll().collectList().block();
        assertThat(albumList).hasSize(databaseSizeBeforeUpdate);
        Album testAlbum = albumList.get(albumList.size() - 1);
        assertThat(testAlbum.getTitle()).isEqualTo(DEFAULT_TITLE);
        assertThat(testAlbum.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testAlbum.getCreated()).isEqualTo(UPDATED_CREATED);
    }

    @Test
    void fullUpdateAlbumWithPatch() throws Exception {
        // Initialize the database
        albumRepository.save(album).block();

        int databaseSizeBeforeUpdate = albumRepository.findAll().collectList().block().size();

        // Update the album using partial update
        Album partialUpdatedAlbum = new Album();
        partialUpdatedAlbum.setId(album.getId());

        partialUpdatedAlbum.title(UPDATED_TITLE).description(UPDATED_DESCRIPTION).created(UPDATED_CREATED);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedAlbum.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedAlbum))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Album in the database
        List<Album> albumList = albumRepository.findAll().collectList().block();
        assertThat(albumList).hasSize(databaseSizeBeforeUpdate);
        Album testAlbum = albumList.get(albumList.size() - 1);
        assertThat(testAlbum.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testAlbum.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testAlbum.getCreated()).isEqualTo(UPDATED_CREATED);
    }

    @Test
    void patchNonExistingAlbum() throws Exception {
        int databaseSizeBeforeUpdate = albumRepository.findAll().collectList().block().size();
        album.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, album.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(album))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Album in the database
        List<Album> albumList = albumRepository.findAll().collectList().block();
        assertThat(albumList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchAlbum() throws Exception {
        int databaseSizeBeforeUpdate = albumRepository.findAll().collectList().block().size();
        album.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(album))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Album in the database
        List<Album> albumList = albumRepository.findAll().collectList().block();
        assertThat(albumList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamAlbum() throws Exception {
        int databaseSizeBeforeUpdate = albumRepository.findAll().collectList().block().size();
        album.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(album))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Album in the database
        List<Album> albumList = albumRepository.findAll().collectList().block();
        assertThat(albumList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteAlbum() {
        // Initialize the database
        albumRepository.save(album).block();

        int databaseSizeBeforeDelete = albumRepository.findAll().collectList().block().size();

        // Delete the album
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, album.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        List<Album> albumList = albumRepository.findAll().collectList().block();
        assertThat(albumList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
