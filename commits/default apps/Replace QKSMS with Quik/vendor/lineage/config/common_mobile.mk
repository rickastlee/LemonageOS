# Inherit common mobile Lineage stuff
$(call inherit-product, vendor/lineage/config/common.mk)

# AOSP packages
PRODUCT_PACKAGES += \
    Email \
    Exchange2

# Lineage packages
PRODUCT_PACKAGES += \
    Backgrounds \
    FossifyGallery \
    Markup \
    MotoDialer \
    Profiles \
    Quik \
    Seedvault \
    Unitto \
    ViaBrowser

ifneq ($(TARGET_EXCLUDES_AUDIOFX),true)
PRODUCT_PACKAGES += \
    AudioFX
endif

ifeq ($(PRODUCT_TYPE), go)
PRODUCT_PACKAGES += \
    TrebuchetQuickStepGo

PRODUCT_DEXPREOPT_SPEED_APPS += \
    TrebuchetQuickStepGo
else
PRODUCT_PACKAGES += \
    TrebuchetQuickStep

PRODUCT_DEXPREOPT_SPEED_APPS += \
    TrebuchetQuickStep
endif

# Accents
PRODUCT_PACKAGES += \
    LineageBlackTheme \
    LineageBlackAccent \
    LineageBlueAccent \
    LineageBrownAccent \
    LineageCyanAccent \
    LineageGreenAccent \
    LineageOrangeAccent \
    LineagePinkAccent \
    LineagePurpleAccent \
    LineageRedAccent \
    LineageYellowAccent

# Audio
$(call inherit-product, vendor/lineage/config/audio.mk)

# Charger
PRODUCT_PACKAGES += \
    charger_res_images

ifneq ($(WITH_LINEAGE_CHARGER),false)
PRODUCT_PACKAGES += \
    lineage_charger_animation
endif

# Customizations
PRODUCT_PACKAGES += \
    IconShapeSquareOverlay \
    NavigationBarMode2ButtonOverlay

# Default permissions
PRODUCT_COPY_FILES += \
    vendor/lineage/config/permissions/default-permissions-lineageos.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/default-permissions/default-permissions-lineageos.xml

# Media
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    media.recorder.show_manufacturer_and_model=true

# SystemUI plugins
PRODUCT_PACKAGES += \
    QuickAccessWallet
