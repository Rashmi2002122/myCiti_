package com.myciti.controller;

import com.myciti.dto.ListingResponse;
import com.myciti.model.Category;
import com.myciti.service.ListingService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    /**
     * GET /api/listings?category=&lat=&lng=&radiusKm=&q=
     * Returns active listings (status = PUBLISHED and today between startDate and endDate).
     * Includes distanceKm sorted ascending when lat/lng are provided.
     */
    @GetMapping
    public ResponseEntity<List<ListingResponse>> getListings(
        @RequestParam(required = false) Category category,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) Double lat,
        @RequestParam(required = false) Double lng,
        @RequestParam(required = false) Double radiusKm,
        @RequestParam(required = false) String q
    ) {
        List<ListingResponse> listings = listingService.getListings(category, city, lat, lng, radiusKm, q);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
            .body(listings);
    }

    /**
     * GET /api/listings/{id}
     * Returns full detail of single listing.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListingById(@PathVariable Long id) {
        ListingResponse listing = listingService.getListingById(id);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(120, TimeUnit.SECONDS).cachePublic())
            .body(listing);
    }

    /**
     * GET /api/listings/{id}/share
     * Open Graph meta tags HTML for WhatsApp / Social Media preview.
     */
    @GetMapping(value = "/{id}/share", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getSharePreview(@PathVariable Long id) {
        ListingResponse listing = listingService.getListingById(id);

        String title = escapeHtml(listing.getTitle());
        String description = escapeHtml(
            (listing.getOfferText() != null && !listing.getOfferText().isEmpty())
                ? listing.getOfferText() + " | " + listing.getLandmark() + ", " + listing.getAddress()
                : listing.getDescription()
        );
        String rawImageUrl = listing.getPrimaryImageUrl();
        String imageUrl;
        if (rawImageUrl != null && (rawImageUrl.startsWith("https://") || rawImageUrl.startsWith("http://"))) {
            imageUrl = escapeHtml(rawImageUrl);
        } else {
            imageUrl = "https://images.unsplash.com/photo-1601614212959-53696884617d?auto=format&fit=crop&w=1200&q=80";
        }

        String timing = escapeHtml(listing.getDailyOpenTime() + " - " + listing.getDailyCloseTime());
        String statusText = escapeHtml(listing.isOpenNow() ? "🟢 Open Now" : "🔴 Closed");
        String category = escapeHtml(listing.getCategory() != null ? listing.getCategory().name() : "");
        String landmark = escapeHtml(listing.getLandmark() != null ? listing.getLandmark() : "");
        String address = escapeHtml(listing.getAddress() != null ? listing.getAddress() : "");

        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>%s - myCite</title>
                <!-- Open Graph / Facebook / WhatsApp -->
                <meta property="og:type" content="website" />
                <meta property="og:title" content="%s (%s)" />
                <meta property="og:description" content="%s | %s | %s" />
                <meta property="og:image" content="%s" />
                <meta property="og:image:width" content="1200" />
                <meta property="og:image:height" content="630" />
                <meta name="twitter:card" content="summary_large_image" />
                <meta name="twitter:title" content="%s" />
                <meta name="twitter:description" content="%s" />
                <meta name="twitter:image" content="%s" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #eaf4ff; color: #1e293b; padding: 24px; text-align: center; }
                    .card { max-width: 500px; margin: 40px auto; background: #ffffff; border-radius: 24px; padding: 24px; box-shadow: 0 10px 25px rgba(0,0,0,0.06); text-align: left; }
                    img { width: 100%%; border-radius: 16px; max-height: 260px; object-fit: cover; }
                    .tag { display: inline-block; padding: 4px 12px; background: #cfe7ff; color: #1d4ed8; border-radius: 9999px; font-size: 12px; font-weight: 600; text-transform: uppercase; margin-top: 12px; }
                    h2 { margin: 12px 0 6px; font-size: 20px; }
                    p { margin: 6px 0; color: #475569; font-size: 14px; line-height: 1.5; }
                    .btn { display: block; margin-top: 18px; padding: 12px 20px; background: #2f7bff; color: #fff; text-decoration: none; border-radius: 16px; font-weight: 600; text-align: center; }
                </style>
            </head>
            <body>
                <div class="card">
                    <img src="%s" alt="%s" />
                    <span class="tag">%s</span>
                    <h2>%s</h2>
                    <p><strong>Status:</strong> %s &bull; %s</p>
                    <p><strong>Venue:</strong> %s, %s</p>
                    <p>%s</p>
                    <a class="btn" href="/?listingId=%d">View on myCite Map</a>
                </div>
            </body>
            </html>
            """.formatted(
                title,
                title, category,
                statusText, timing, description,
                imageUrl,
                title, description, imageUrl,
                imageUrl, title,
                category,
                title,
                statusText, timing,
                landmark, address,
                description,
                listing.getId()
            );

        return ResponseEntity.ok(html);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
