package br.com.officecom.image.controller;

import br.com.officecom.image.service.ImageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/convert")
    public Mono<String> convertImage(@RequestParam String key) {
        return imageService.processImage(key)
                .then(Mono.just("Image converted and saved to webp folder successfully"));
    }

    @PostMapping("/convert-all")
    public Mono<String> convertAllImages() {
        return imageService.processAllImages()
                .then(Mono.just("Started processing all images to webp"));
    }

    @GetMapping("/list")
    public Mono<List<String>> listImages() {
        return imageService.listObjects();
    }
}
