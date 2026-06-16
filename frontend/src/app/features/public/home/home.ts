import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { ThemeStore } from '@app/core/theme/theme.store';
import { AuthDialogComponent } from '@app/features/auth/components/auth-dialog/auth-dialog';
import { IconComponent } from '@app/shared/components/icon/icon';

type HomeLanguage = 'en' | 'bn';
type AuthTab = 'login' | 'register';

interface NavItem {
  readonly label: string;
  readonly href: string;
  readonly hasDropdown?: boolean;
}

interface IconCard {
  readonly icon: string;
  readonly title: string;
  readonly description: string;
}

interface WorkflowStep extends IconCard {
  readonly step: string;
}

interface PlatformBadge {
  readonly name: string;
  readonly icon: string;
  readonly accentClass: string;
}

interface DashboardMetric {
  readonly label: string;
  readonly value: string;
}

interface ConversionExample {
  readonly id: string;
  readonly rawLabel: string;
  readonly creativeLabel: string;
  readonly productName: string;
  readonly productType: 'apparel' | 'skincare' | 'coffee';
  readonly rawImage?: string;
  readonly creativeImage?: string;
  readonly campaignTitle: string;
  readonly campaignSubtitle: string;
  readonly cta: string;
  readonly platform: string;
  readonly tone: string;
  readonly progress: number;
  readonly stages: readonly string[];
  readonly accentClass: string;
}

interface HomeCopy {
  readonly nav: {
    readonly home: string;
    readonly features: string;
    readonly resources: string;
    readonly about: string;
    readonly login: string;
    readonly dashboard: string;
    readonly theme: string;
    readonly language: string;
  };
  readonly hero: {
    readonly eyebrow: string;
    readonly headline: string;
    readonly highlight: string;
    readonly description: string;
    readonly primaryCta: string;
    readonly secondaryCta: string;
    readonly proofChips: readonly string[];
    readonly rawProduct: string;
    readonly rawBadge: string;
    readonly aiProcessing: string;
    readonly campaignCreative: string;
    readonly processingStages: readonly string[];
  };
  readonly platformStrip: {
    readonly title: string;
  };
  readonly workflow: {
    readonly title: string;
    readonly description: string;
    readonly steps: readonly WorkflowStep[];
  };
  readonly transformation: {
    readonly title: string;
    readonly description: string;
    readonly cta: string;
  };
  readonly features: {
    readonly title: string;
    readonly cards: readonly IconCard[];
  };
  readonly dashboard: {
    readonly title: string;
    readonly subtitle: string;
    readonly metrics: readonly DashboardMetric[];
  };
  readonly audience: {
    readonly title: string;
    readonly cards: readonly IconCard[];
  };
  readonly trust: {
    readonly title: string;
    readonly items: readonly IconCard[];
  };
  readonly about: {
    readonly eyebrow: string;
    readonly title: string;
    readonly description: string;
  };
  readonly finalCta: {
    readonly title: string;
    readonly subtitle: string;
  };
  readonly footer: {
    readonly note: string;
  };
}

const PLATFORMS: readonly PlatformBadge[] = [
  { name: 'Facebook', icon: 'facebook', accentClass: 'platform-facebook' },
  { name: 'Instagram', icon: 'instagram', accentClass: 'platform-instagram' },
  { name: 'TikTok', icon: 'tiktok', accentClass: 'platform-tiktok' },
  { name: 'LinkedIn', icon: 'linkedin', accentClass: 'platform-linkedin' },
  { name: 'YouTube Shorts', icon: 'youtube-shorts', accentClass: 'platform-youtube' },
  { name: 'Google Ads', icon: 'google-ads', accentClass: 'platform-google' },
  { name: 'Marketplace', icon: 'marketplace', accentClass: 'platform-market' },
  { name: 'And more', icon: 'more', accentClass: 'platform-more' },
];

const CONVERSION_EXAMPLES: readonly ConversionExample[] = [
  {
    id: 'apparel',
    rawLabel: 'Raw apparel photo',
    creativeLabel: 'Fashion launch ad',
    productName: 'Premium Sweatshirt',
    productType: 'apparel',
    rawImage: '/assets/hero-raw-product-card.png',
    creativeImage: '/assets/hero-campaign-creative-card.png',
    campaignTitle: 'Elevate Your Brand Style',
    campaignSubtitle: 'Premium quality. Timeless design. Made for impact.',
    cta: 'Shop Now',
    platform: 'Instagram Feed',
    tone: 'Premium fashion',
    progress: 75,
    stages: ['Reading fabric and color cues', 'Applying fashion brand voice', 'Preparing Instagram layout'],
    accentClass: 'conversion-apparel',
  },
  {
    id: 'skincare',
    rawLabel: 'Raw skincare pack',
    creativeLabel: 'Beauty campaign ad',
    productName: 'Hydra Glow Serum',
    productType: 'skincare',
    rawImage: '/assets/homepage-skincare-raw.png',
    creativeImage: '/assets/homepage-skincare-creative.png',
    campaignTitle: 'Glow Starts Here',
    campaignSubtitle: 'Clean skincare visuals with soft launch messaging.',
    cta: 'Try the Serum',
    platform: 'Facebook Ad',
    tone: 'Clean beauty',
    progress: 82,
    stages: ['Detecting bottle silhouette', 'Matching clean beauty tone', 'Writing benefit-led copy'],
    accentClass: 'conversion-skincare',
  },
  {
    id: 'coffee',
    rawLabel: 'Raw coffee package',
    creativeLabel: 'Marketplace creative',
    productName: 'Roasted Coffee Pack',
    productType: 'coffee',
    campaignTitle: 'Fresh Roast, Bold Start',
    campaignSubtitle: 'Marketplace-ready product ad with clear offer copy.',
    cta: 'Order Today',
    platform: 'Marketplace',
    tone: 'Warm retail',
    progress: 88,
    stages: ['Reading package shape', 'Building marketplace offer', 'Exporting product-first creative'],
    accentClass: 'conversion-coffee',
  },
];

const CONVERSION_ROTATION_MS = 5 * 60 * 1000;

const EN_COPY: HomeCopy = {
  nav: {
    home: 'Home',
    features: 'Features',
    resources: 'Resources',
    about: 'About Us',
    login: 'Login',
    dashboard: 'Dashboard',
    theme: 'Theme',
    language: 'Language',
  },
  hero: {
    eyebrow: 'AI Creative Operating System',
    headline: 'Turn Raw Product Photos Into',
    highlight: 'Ready-to-Publish Ads',
    description:
      'Lebhas helps brands, agencies, and marketing teams generate campaign creatives, captions, ad copy, hashtags, and platform-ready assets from one workspace.',
    primaryCta: 'Get Started',
    secondaryCta: 'See How It Works',
    proofChips: ['No design skill needed', 'Brand-safe outputs', 'Multi-platform ready'],
    rawProduct: 'Raw Product',
    rawBadge: 'Product image',
    aiProcessing: 'AI Processing',
    campaignCreative: 'Campaign Creative',
    processingStages: ['Reading product details', 'Applying brand context', 'Preparing platform formats'],
  },
  platformStrip: {
    title: 'Publish across all major platforms',
  },
  workflow: {
    title: 'AI Creative Workflow',
    description: 'From product upload to approved campaign output in one connected workspace.',
    steps: [
      { step: '01', icon: 'upload-cloud', title: 'Upload Product', description: 'Add raw product images from your library.' },
      { step: '02', icon: 'palette', title: 'Add Brand Context', description: 'Keep tone, colors, language, and audience consistent.' },
      { step: '03', icon: 'monitor-smartphone', title: 'Choose Platform', description: 'Select Facebook, Instagram, TikTok, LinkedIn, or more.' },
      { step: '04', icon: 'sparkles', title: 'Generate Creative', description: 'Create multiple campaign-ready variations.' },
      { step: '05', icon: 'badge-check', title: 'Review & Approve', description: 'Collaborate with your team before publishing.' },
      { step: '06', icon: 'download', title: 'Export & Publish', description: 'Download, share, or prepare assets for campaigns.' },
    ],
  },
  transformation: {
    title: 'From Raw Product to Campaign Creative',
    description:
      'Upload a simple product photo and let Lebhas create campaign-ready visuals, captions, and ad directions for your brand.',
    cta: 'Try with your product',
  },
  features: {
    title: 'Everything your brand needs to create faster',
    cards: [
      { icon: 'wand-sparkles', title: 'Campaign Creative Generator', description: 'Create platform-ready campaign visuals from product and brand context.' },
      { icon: 'layout-grid', title: 'Post Generator', description: 'Generate social media post ideas for multiple platforms.' },
      { icon: 'captions', title: 'Caption Generator', description: 'Write captions that match your tone and campaign goal.' },
      { icon: 'megaphone', title: 'Ad Copy Generator', description: 'Create persuasive headlines and ad text.' },
      { icon: 'folder-open', title: 'Asset Library', description: 'Organize product images, logos, and campaign media.' },
      { icon: 'route', title: 'Approval Workflow', description: 'Review, approve, reject, and manage creative versions.' },
    ],
  },
  dashboard: {
    title: 'Built around your creative workspace',
    subtitle: 'Manage product assets, generation flow, approvals, credits, and ready-to-export campaign outputs in one SaaS workspace.',
    metrics: [
      { label: 'Credits', value: '1,240' },
      { label: 'Plan', value: 'Growth' },
      { label: 'Pending approvals', value: '06' },
    ],
  },
  audience: {
    title: 'Made for brands and teams that need campaign speed',
    cards: [
      { icon: 'gem', title: 'Fashion brands', description: 'Launch seasonal creative without slowing down design teams.' },
      { icon: 'package-open', title: 'E-commerce sellers', description: 'Turn product shots into campaign assets for every channel.' },
      { icon: 'building-2', title: 'Local businesses', description: 'Create polished ads without a full creative department.' },
      { icon: 'briefcase', title: 'Marketing agencies', description: 'Scale campaign variations across clients and platforms.' },
      { icon: 'pencil-line', title: 'Freelancers', description: 'Deliver stronger creative concepts faster.' },
      { icon: 'users', title: 'Social media teams', description: 'Generate copy, captions, hashtags, and visual directions together.' },
    ],
  },
  trust: {
    title: 'Designed for brand-safe creative operations',
    items: [
      { icon: 'shield-check', title: 'Workspace-based access', description: 'Keep brand and campaign work scoped by workspace.' },
      { icon: 'circle-check', title: 'Approval before publishing', description: 'Review generated versions before campaign use.' },
      { icon: 'lock-keyhole', title: 'Private asset storage foundation', description: 'Build campaigns from controlled product and brand media.' },
      { icon: 'wallet-cards', title: 'Usage and credit visibility', description: 'Track generation usage and credit movement.' },
      { icon: 'route', title: 'Provider-independent AI architecture', description: 'Support multiple AI provider foundations as the platform evolves.' },
    ],
  },
  about: {
    eyebrow: 'About Us',
    title: 'Creative speed with brand control',
    description:
      'Lebhas focuses on the space between raw product assets and campaign-ready execution, giving teams one place to generate, review, approve, and prepare creatives for launch.',
  },
  finalCta: {
    title: 'Ready to create ads beyond imagination?',
    subtitle: 'Start with one product image and build campaign-ready creative assets in minutes.',
  },
  footer: {
    note: 'Built for brands, agencies, and marketing teams.',
  },
};

const BN_COPY: HomeCopy = {
  ...EN_COPY,
  nav: {
    ...EN_COPY.nav,
    home: 'হোম',
    features: 'ফিচার',
    resources: 'রিসোর্স',
    about: 'আমাদের সম্পর্কে',
    login: 'লগইন',
    dashboard: 'ড্যাশবোর্ড',
    language: 'ভাষা',
  },
  hero: {
    ...EN_COPY.hero,
    eyebrow: 'এআই ক্রিয়েটিভ অপারেটিং সিস্টেম',
    headline: 'র প্রোডাক্ট ছবি থেকে',
    highlight: 'রেডি-টু-পাবলিশ বিজ্ঞাপন',
    description:
      'Lebhas ব্র্যান্ড, এজেন্সি ও মার্কেটিং টিমকে এক ওয়ার্কস্পেস থেকে ক্যাম্পেইন ক্রিয়েটিভ, ক্যাপশন, অ্যাড কপি, হ্যাশট্যাগ ও প্ল্যাটফর্ম-রেডি অ্যাসেট তৈরি করতে সাহায্য করে।',
    primaryCta: 'শুরু করুন',
    secondaryCta: 'কীভাবে কাজ করে দেখুন',
    proofChips: ['ডিজাইন স্কিল ছাড়াই', 'বাংলা + ইংরেজি', 'ব্র্যান্ড-সেফ আউটপুট', 'মাল্টি-প্ল্যাটফর্ম রেডি'],
  },
  platformStrip: {
    title: 'সব প্রধান প্ল্যাটফর্মের জন্য প্রস্তুত',
  },
  workflow: {
    ...EN_COPY.workflow,
    title: 'AI Creative Workflow',
    description: 'প্রোডাক্ট আপলোড থেকে অনুমোদিত ক্যাম্পেইন আউটপুট পর্যন্ত এক সংযুক্ত ওয়ার্কস্পেস।',
  },
  transformation: {
    ...EN_COPY.transformation,
    title: 'র প্রোডাক্ট থেকে ক্যাম্পেইন ক্রিয়েটিভ',
    cta: 'আপনার প্রোডাক্ট দিয়ে চেষ্টা করুন',
  },
  features: {
    ...EN_COPY.features,
    title: 'দ্রুত ক্রিয়েটিভ বানাতে আপনার ব্র্যান্ডের যা দরকার',
  },
  dashboard: {
    ...EN_COPY.dashboard,
    title: 'আপনার ক্রিয়েটিভ ওয়ার্কস্পেসকে কেন্দ্র করে তৈরি',
  },
  audience: {
    ...EN_COPY.audience,
    title: 'যেসব ব্র্যান্ড ও টিম দ্রুত ক্যাম্পেইন চালাতে চায়',
  },
  trust: {
    ...EN_COPY.trust,
    title: 'ব্র্যান্ড-সেফ ক্রিয়েটিভ অপারেশনের জন্য ডিজাইন করা',
  },
  about: {
    eyebrow: 'আমাদের সম্পর্কে',
    title: 'ব্র্যান্ড নিয়ন্ত্রণ রেখে দ্রুত ক্রিয়েটিভ',
    description:
      'Lebhas র প্রোডাক্ট অ্যাসেট থেকে ক্যাম্পেইন-রেডি এক্সিকিউশন পর্যন্ত কাজকে এক জায়গায় আনে, যাতে টিম দ্রুত জেনারেট, রিভিউ, অনুমোদন ও লঞ্চের প্রস্তুতি নিতে পারে।',
  },
  finalCta: {
    title: 'কল্পনার বাইরে বিজ্ঞাপন তৈরি করতে প্রস্তুত?',
    subtitle: 'একটি প্রোডাক্ট ছবি দিয়ে শুরু করুন এবং মিনিটে ক্যাম্পেইন-রেডি ক্রিয়েটিভ অ্যাসেট তৈরি করুন।',
  },
};

const HOME_COPY: Record<HomeLanguage, HomeCopy> = {
  en: EN_COPY,
  bn: BN_COPY,
};

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [AuthDialogComponent, IconComponent, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);
  private readonly router = inject(Router);
  private readonly auth = inject(CurrentUserStore);
  private readonly themeStore = inject(ThemeStore);

  protected readonly currentYear = new Date().getFullYear();
  protected readonly isAuthenticated = this.auth.isAuthenticated;
  protected readonly currentTheme = this.themeStore.theme;
  protected readonly loginModalOpen = signal(false);
  protected readonly authInitialTab = signal<AuthTab>('login');
  protected readonly menuOpen = signal(false);
  protected readonly language = signal<HomeLanguage>('en');
  protected readonly activeConversionIndex = signal(0);

  protected readonly copy = computed(() => HOME_COPY[this.language()]);
  protected readonly activeConversion = computed(
    () => CONVERSION_EXAMPLES[this.activeConversionIndex() % CONVERSION_EXAMPLES.length],
  );
  protected readonly themeToggleIcon = computed(() => (this.currentTheme() === 'dark' ? 'sun' : 'moon'));
  protected readonly themeToggleLabel = computed(() =>
    this.currentTheme() === 'dark' ? 'Switch to light mode' : 'Switch to dark mode',
  );

  protected readonly navItems = computed<readonly NavItem[]>(() => [
    { label: this.copy().nav.home, href: '#home' },
    { label: this.copy().nav.features, href: '#features' },
    { label: this.copy().nav.resources, href: '#resources', hasDropdown: true },
    { label: this.copy().nav.about, href: '#about' },
  ]);

  protected readonly platformBadges = PLATFORMS;

  constructor() {
    const searchParams = new URLSearchParams(globalThis.location.search);
    const openedFromLoginRoute = globalThis.location.pathname === '/login';
    const openedFromQuery = searchParams.get('auth') === 'login';

    this.loginModalOpen.set(openedFromLoginRoute || openedFromQuery);

    const conversionTimer = globalThis.setInterval(() => {
      this.nextConversion();
    }, CONVERSION_ROTATION_MS);

    this.destroyRef.onDestroy(() => globalThis.clearInterval(conversionTimer));
  }

  private nextConversion(): void {
    this.activeConversionIndex.update((index) => (index + 1) % CONVERSION_EXAMPLES.length);
  }

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  protected openLoginModal(): void {
    this.closeMenu();

    if (this.isAuthenticated()) {
      void this.router.navigateByUrl('/dashboard');
      return;
    }

    this.authInitialTab.set('login');
    this.loginModalOpen.set(true);
  }

  protected openRegisterModal(): void {
    this.closeMenu();

    if (this.isAuthenticated()) {
      void this.router.navigateByUrl('/dashboard');
      return;
    }

    this.authInitialTab.set('register');
    this.loginModalOpen.set(true);
  }

  protected toggleTheme(): void {
    this.themeStore.toggleTheme();
  }

  protected setLanguage(language: HomeLanguage): void {
    this.language.set(language);
  }

  protected closeLoginModal(): void {
    this.loginModalOpen.set(false);

    if (
      globalThis.location.pathname === '/login' ||
      new URLSearchParams(globalThis.location.search).has('returnUrl')
    ) {
      void this.router.navigate(['/'], {
        queryParams: { returnUrl: null, auth: null },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      });
    }
  }

  protected scrollToSection(sectionId: string): void {
    this.closeMenu();

    this.document.getElementById(sectionId)?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    });
  }

  protected platformIconPath(icon: string): string {
    return `/assets/icons/platforms/${icon || 'more'}.svg`;
  }

  protected useFallbackPlatformIcon(event: Event): void {
    const image = event.target as HTMLImageElement | null;
    const fallbackPath = this.platformIconPath('more');

    if (image && !image.src.endsWith(fallbackPath)) {
      image.src = fallbackPath;
    }
  }
}
