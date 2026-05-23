import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { ThemeStore } from '@app/core/theme/theme.store';
import { AuthDialogComponent } from '@app/features/auth/components/auth-dialog/auth-dialog';
import { IconComponent } from '@app/shared/components/icon/icon';
import { ThemeToggleComponent } from '@app/shared/components/theme-toggle/theme-toggle';

type HomeLanguage = 'en' | 'bn';

interface NavItem {
  readonly label: string;
  readonly href: string;
  readonly hasDropdown?: boolean;
}

interface HeroHighlight {
  readonly icon: string;
  readonly title: string;
}

interface OutcomeChip {
  readonly icon: string;
  readonly label: string;
}

interface PlatformBadge {
  readonly name: string;
  readonly shortLabel: string;
  readonly accentClass: string;
}

interface WorkflowStep {
  readonly step: string;
  readonly title: string;
  readonly description: string;
  readonly icon: string;
}

interface WorkspaceMetric {
  readonly value: string;
  readonly delta: string;
  readonly label: string;
}

interface ActivityItem {
  readonly title: string;
  readonly age: string;
  readonly icon: string;
}

interface TopPlatform {
  readonly name: string;
  readonly share: number;
  readonly color: string;
}

interface PricingPlan {
  readonly name: string;
  readonly price: string;
  readonly period: string;
  readonly description: string;
  readonly features: readonly string[];
  readonly action: string;
  readonly featured?: boolean;
}

interface AboutPoint {
  readonly icon: string;
  readonly title: string;
  readonly description: string;
}

interface AboutStat {
  readonly value: string;
  readonly label: string;
}

interface HomeCopy {
  readonly nav: {
    readonly home: string;
    readonly features: string;
    readonly pricing: string;
    readonly resources: string;
    readonly about: string;
    readonly login: string;
    readonly dashboard: string;
    readonly theme: string;
    readonly language: string;
  };
  readonly hero: {
    readonly eyebrow: string;
    readonly lineOne: string;
    readonly lineTwo: string;
    readonly accentOne: string;
    readonly accentTwo: string;
    readonly description: string;
    readonly primaryCta: string;
    readonly secondaryCta: string;
    readonly rawProduct: string;
    readonly rawBadge: string;
    readonly aiProcessing: string;
    readonly campaignCreative: string;
    readonly highlights: readonly HeroHighlight[];
    readonly processingStages: readonly string[];
    readonly outcomes: readonly OutcomeChip[];
  };
  readonly platformStrip: {
    readonly title: string;
    readonly moreLabel: string;
  };
  readonly workflow: {
    readonly title: string;
    readonly description: string;
    readonly steps: readonly WorkflowStep[];
  };
  readonly workspace: {
    readonly title: string;
    readonly period: string;
    readonly metrics: readonly WorkspaceMetric[];
    readonly activityTitle: string;
    readonly activities: readonly ActivityItem[];
    readonly chartTitle: string;
  };
  readonly pricing: {
    readonly eyebrow: string;
    readonly title: string;
    readonly description: string;
    readonly plans: readonly PricingPlan[];
  };
  readonly about: {
    readonly eyebrow: string;
    readonly title: string;
    readonly description: string;
    readonly paragraphs: readonly string[];
    readonly points: readonly AboutPoint[];
    readonly stats: readonly AboutStat[];
    readonly primaryCta: string;
    readonly secondaryCta: string;
  };
  readonly footer: string;
}

const TOP_PLATFORMS: readonly TopPlatform[] = [
  { name: 'Facebook', share: 45, color: '#1d9bf0' },
  { name: 'Instagram', share: 30, color: '#a3a3a3' },
  { name: 'TikTok', share: 15, color: '#22d3ee' },
  { name: 'LinkedIn', share: 10, color: '#38bdf8' },
];

const HOME_COPY: Record<HomeLanguage, HomeCopy> = {
  en: {
    nav: {
      home: 'Home',
      features: 'Features',
      pricing: 'Pricing',
      resources: 'Resources',
      about: 'About Us',
      login: 'Login',
      dashboard: 'Dashboard',
      theme: 'Theme',
      language: 'Language',
    },
    hero: {
      eyebrow: 'AI Creative Operating System',
      lineOne: 'Transform Raw Product',
      lineTwo: 'Images Into',
      accentOne: 'High-Converting',
      accentTwo: 'Campaign Creatives',
      description:
        'Lebhas - Brand Attire is an AI-powered creative operating system that helps brands, agencies, and marketing teams produce on-brand, platform-ready ads in minutes, not days.',
      primaryCta: 'Start Creating',
      secondaryCta: 'See How It Works',
      rawProduct: 'Raw Product',
      rawBadge: 'RAW IMAGE',
      aiProcessing: 'AI Processing',
      campaignCreative: 'Campaign Creative',
      highlights: [
        { icon: 'sparkles', title: 'AI-Powered Intelligence' },
        { icon: 'users', title: 'Workspace Based' },
        { icon: 'globe', title: 'Multi-Platform Ready' },
        { icon: 'shield-check', title: 'Secure & Scalable' },
      ],
      processingStages: [
        'Analyzing product...',
        'Understanding brand...',
        'Generating creatives...',
      ],
      outcomes: [
        { icon: 'shield-check', label: 'Brand Consistency' },
        { icon: 'rocket', label: 'Faster Time to Market' },
        { icon: 'zap', label: 'Higher Engagement' },
        { icon: 'bar-chart-3', label: 'Better ROI' },
      ],
    },
    platformStrip: {
      title: 'Publish across all major platforms',
      moreLabel: 'And more',
    },
    workflow: {
      title: 'AI Creative Workflow',
      description: 'End-to-end creative production pipeline for modern marketing teams',
      steps: [
        {
          step: '01',
          title: 'Upload or Select Raw Product',
          description: 'Add product images from your library.',
          icon: 'image',
        },
        {
          step: '02',
          title: 'AI Prompt Intelligence',
          description: 'AI understands your brand, product, and campaign goal.',
          icon: 'box',
        },
        {
          step: '03',
          title: 'Generate Creatives',
          description: 'AI creates multiple high-impact variations.',
          icon: 'sparkles',
        },
        {
          step: '04',
          title: 'Review and Approve',
          description: 'Collaborate, review, and approve the best creatives.',
          icon: 'share-2',
        },
        {
          step: '05',
          title: 'Export and Publish',
          description: 'Export in any format and publish across platforms.',
          icon: 'sun',
        },
      ],
    },
    workspace: {
      title: 'Workspace Overview',
      period: 'This Month',
      metrics: [
        { value: '24', delta: '+12%', label: 'Brands' },
        { value: '156', delta: '+18%', label: 'Products / Services' },
        { value: '78', delta: '+8%', label: 'Projects / Campaigns' },
        { value: '342', delta: '+23%', label: 'Creative Requests' },
      ],
      activityTitle: 'Recent Activity',
      activities: [
        { title: 'New campaign "Winter Collection" created', age: '2h ago', icon: 'target' },
        { title: '10 creatives generated for "Summer Sale"', age: '6h ago', icon: 'target' },
        { title: 'Creative "Instagram Poster 01" approved', age: '1d ago', icon: 'circle-check' },
      ],
      chartTitle: 'Top Platforms',
    },
    pricing: {
      eyebrow: 'Pricing',
      title: 'Plans for teams that need creative velocity',
      description:
        'Choose the workspace that matches your approval flow, campaign volume, and publishing needs.',
      plans: [
        {
          name: 'Starter',
          price: '$29',
          period: '/month',
          description: 'Best for solo marketers validating fast campaign production.',
          features: [
            '1 workspace',
            '50 AI creative generations per month',
            'Brand kit and prompt presets',
            'Manual export for all supported formats',
          ],
          action: 'Start Starter',
        },
        {
          name: 'Growth',
          price: '$79',
          period: '/month',
          description: 'Built for growing teams running multi-platform campaign cycles.',
          features: [
            '5 workspaces with shared review flow',
            '250 AI creative generations per month',
            'Approval history and collaboration notes',
            'Priority export queue and reusable templates',
          ],
          action: 'Choose Growth',
          featured: true,
        },
        {
          name: 'Enterprise',
          price: 'Custom',
          period: '',
          description: 'For brands and agencies that need scale, governance, and support.',
          features: [
            'Unlimited workspaces',
            'Custom generation volume and roles',
            'Dedicated onboarding and security review',
            'API and workflow integration support',
          ],
          action: 'Talk to Sales',
        },
      ],
    },
    about: {
      eyebrow: 'About Us',
      title: 'Built for brands that need speed without losing control',
      description:
        'Lebhas focuses on the gap between raw product assets and campaign-ready creative execution.',
      paragraphs: [
        'The platform is designed for teams that need to move from raw images to approved creative output with less back-and-forth. Brand context, review flow, and publishing readiness stay connected in one workspace.',
        'That means faster launches, cleaner collaboration, and output that stays aligned with brand standards even when campaign volume increases.',
      ],
      points: [
        {
          icon: 'sparkles',
          title: 'Creative intelligence with guardrails',
          description: 'Generate faster while keeping tone, product context, and brand direction consistent.',
        },
        {
          icon: 'users',
          title: 'Team-ready workflow',
          description: 'Support marketing, design, and approval stakeholders in the same operating flow.',
        },
        {
          icon: 'shield-check',
          title: 'Operational reliability',
          description: 'Keep asset review, export decisions, and platform readiness visible for the whole team.',
        },
      ],
      stats: [
        { value: '10x', label: 'Faster concept turnaround' },
        { value: '4', label: 'Primary publishing channels' },
        { value: '1', label: 'Connected creative workspace' },
      ],
      primaryCta: 'Explore Workflow',
      secondaryCta: 'View Pricing',
    },
    footer: 'LEBHAS - Create Ads Beyond Imagination. All rights reserved.',
  },
  bn: {
    nav: {
      home: 'হোম',
      features: 'ফিচার',
      pricing: 'প্রাইসিং',
      resources: 'রিসোর্স',
      about: 'আমাদের সম্পর্কে',
      login: 'লগইন',
      dashboard: 'ড্যাশবোর্ড',
      theme: 'থিম',
      language: 'ভাষা',
    },
    hero: {
      eyebrow: 'এআই ক্রিয়েটিভ অপারেটিং সিস্টেম',
      lineOne: 'র' + "'" + ' প্রোডাক্ট ইমেজকে',
      lineTwo: 'রূপ দিন',
      accentOne: 'হাই-কনভার্টিং',
      accentTwo: 'ক্যাম্পেইন ক্রিয়েটিভে',
      description:
        'Lebhas - Brand Attire একটি এআই-চালিত ক্রিয়েটিভ অপারেটিং সিস্টেম, যা ব্র্যান্ড, এজেন্সি এবং মার্কেটিং টিমকে মিনিটের মধ্যে অন-ব্র্যান্ড ও প্ল্যাটফর্ম-রেডি বিজ্ঞাপন তৈরি করতে সাহায্য করে।',
      primaryCta: 'ক্রিয়েটিভ শুরু করুন',
      secondaryCta: 'কীভাবে কাজ করে দেখুন',
      rawProduct: 'র' + "'" + ' প্রোডাক্ট',
      rawBadge: 'র' + "'" + ' ইমেজ',
      aiProcessing: 'এআই প্রসেসিং',
      campaignCreative: 'ক্যাম্পেইন ক্রিয়েটিভ',
      highlights: [
        { icon: 'sparkles', title: 'এআই-চালিত ইন্টেলিজেন্স' },
        { icon: 'users', title: 'ওয়ার্কস্পেস ভিত্তিক' },
        { icon: 'globe', title: 'মাল্টি-প্ল্যাটফর্ম রেডি' },
        { icon: 'shield-check', title: 'সিকিউর ও স্কেলেবল' },
      ],
      processingStages: [
        'প্রোডাক্ট বিশ্লেষণ করা হচ্ছে...',
        'ব্র্যান্ড বুঝে নেওয়া হচ্ছে...',
        'ক্রিয়েটিভ তৈরি করা হচ্ছে...',
      ],
      outcomes: [
        { icon: 'shield-check', label: 'ব্র্যান্ড কনসিস্টেন্সি' },
        { icon: 'rocket', label: 'দ্রুততর গো-টু-মার্কেট' },
        { icon: 'zap', label: 'উচ্চতর এনগেজমেন্ট' },
        { icon: 'bar-chart-3', label: 'ভালো ROI' },
      ],
    },
    platformStrip: {
      title: 'সব প্রধান প্ল্যাটফর্মে প্রচার করুন',
      moreLabel: 'আরও অনেক',
    },
    workflow: {
      title: 'এআই ক্রিয়েটিভ ওয়ার্কফ্লো',
      description: 'আধুনিক মার্কেটিং টিমের জন্য শুরু থেকে শেষ পর্যন্ত ক্রিয়েটিভ প্রোডাকশন পাইপলাইন',
      steps: [
        {
          step: '01',
          title: 'র' + "'" + ' প্রোডাক্ট আপলোড বা সিলেক্ট করুন',
          description: 'আপনার লাইব্রেরি থেকে প্রোডাক্ট ইমেজ যোগ করুন।',
          icon: 'image',
        },
        {
          step: '02',
          title: 'এআই প্রম্পট ইন্টেলিজেন্স',
          description: 'এআই আপনার ব্র্যান্ড, প্রোডাক্ট এবং ক্যাম্পেইন লক্ষ্য বুঝে নেয়।',
          icon: 'box',
        },
        {
          step: '03',
          title: 'ক্রিয়েটিভ জেনারেট করুন',
          description: 'এআই একাধিক উচ্চ-প্রভাবশালী ভ্যারিয়েশন তৈরি করে।',
          icon: 'sparkles',
        },
        {
          step: '04',
          title: 'রিভিউ ও অনুমোদন',
          description: 'সহযোগিতার মাধ্যমে সেরা ক্রিয়েটিভ বেছে অনুমোদন দিন।',
          icon: 'share-2',
        },
        {
          step: '05',
          title: 'এক্সপোর্ট ও প্রকাশ',
          description: 'যেকোনো ফরম্যাটে এক্সপোর্ট করে বিভিন্ন প্ল্যাটফর্মে প্রকাশ করুন।',
          icon: 'sun',
        },
      ],
    },
    workspace: {
      title: 'ওয়ার্কস্পেস ওভারভিউ',
      period: 'এই মাস',
      metrics: [
        { value: '24', delta: '+12%', label: 'ব্র্যান্ড' },
        { value: '156', delta: '+18%', label: 'প্রোডাক্ট / সার্ভিস' },
        { value: '78', delta: '+8%', label: 'প্রজেক্ট / ক্যাম্পেইন' },
        { value: '342', delta: '+23%', label: 'ক্রিয়েটিভ রিকোয়েস্ট' },
      ],
      activityTitle: 'সাম্প্রতিক কার্যক্রম',
      activities: [
        { title: 'নতুন "Winter Collection" ক্যাম্পেইন তৈরি হয়েছে', age: '২ ঘন্টা আগে', icon: 'target' },
        { title: '"Summer Sale" এর জন্য ১০টি ক্রিয়েটিভ তৈরি হয়েছে', age: '৬ ঘন্টা আগে', icon: 'target' },
        { title: '"Instagram Poster 01" ক্রিয়েটিভ অনুমোদিত হয়েছে', age: '১ দিন আগে', icon: 'circle-check' },
      ],
      chartTitle: 'শীর্ষ প্ল্যাটফর্ম',
    },
    pricing: {
      eyebrow: 'প্রাইসিং',
      title: 'যে টিম দ্রুত ক্রিয়েটিভ চালাতে চায় তাদের জন্য পরিকল্পনা',
      description:
        'আপনার ওয়ার্কস্পেস, অনুমোদন প্রক্রিয়া এবং ক্যাম্পেইন ভলিউম অনুযায়ী উপযুক্ত প্ল্যান বেছে নিন।',
      plans: [
        {
          name: 'Starter',
          price: '$29',
          period: '/মাস',
          description: 'একজন মার্কেটারের দ্রুত ক্যাম্পেইন প্রোডাকশন যাচাইয়ের জন্য।',
          features: [
            '১টি ওয়ার্কস্পেস',
            'প্রতি মাসে ৫০টি এআই ক্রিয়েটিভ জেনারেশন',
            'ব্র্যান্ড কিট ও প্রম্পট প্রিসেট',
            'সব সাপোর্টেড ফরম্যাটে ম্যানুয়াল এক্সপোর্ট',
          ],
          action: 'Starter শুরু করুন',
        },
        {
          name: 'Growth',
          price: '$79',
          period: '/মাস',
          description: 'বর্ধনশীল টিমের জন্য, যারা একাধিক প্ল্যাটফর্মে ক্যাম্পেইন চালায়।',
          features: [
            '৫টি ওয়ার্কস্পেস ও শেয়ারড রিভিউ ফ্লো',
            'প্রতি মাসে ২৫০টি এআই ক্রিয়েটিভ জেনারেশন',
            'অ্যাপ্রুভাল হিস্ট্রি ও কোলাবোরেশন নোট',
            'প্রায়োরিটি এক্সপোর্ট কিউ ও পুনর্ব্যবহারযোগ্য টেমপ্লেট',
          ],
          action: 'Growth বেছে নিন',
          featured: true,
        },
        {
          name: 'Enterprise',
          price: 'Custom',
          period: '',
          description: 'বড় ব্র্যান্ড ও এজেন্সির জন্য, যেখানে স্কেল ও গভর্নেন্স দরকার।',
          features: [
            'আনলিমিটেড ওয়ার্কস্পেস',
            'কাস্টম জেনারেশন ভলিউম ও রোলস',
            'ডেডিকেটেড অনবোর্ডিং ও সিকিউরিটি রিভিউ',
            'API ও ওয়ার্কফ্লো ইন্টিগ্রেশন সাপোর্ট',
          ],
          action: 'সেলস টিমের সাথে কথা বলুন',
        },
      ],
    },
    about: {
      eyebrow: 'আমাদের সম্পর্কে',
      title: 'যেসব ব্র্যান্ড গতি চায় কিন্তু নিয়ন্ত্রণ হারাতে চায় না, তাদের জন্য তৈরি',
      description:
        'Lebhas কাঁচা প্রোডাক্ট অ্যাসেট থেকে ক্যাম্পেইন-রেডি ক্রিয়েটিভ এক্সিকিউশন পর্যন্ত ব্যবধানটি কমায়।',
      paragraphs: [
        'এই প্ল্যাটফর্ম এমন টিমের জন্য তৈরি, যাদের দ্রুত র' + "'" + ' ইমেজ থেকে অনুমোদিত ক্রিয়েটিভ আউটপুটে যেতে হয়। ব্র্যান্ড কনটেক্সট, রিভিউ ফ্লো এবং পাবলিশিং রেডিনেস একই ওয়ার্কস্পেসে যুক্ত থাকে।',
        'ফলে দ্রুত লঞ্চ, পরিষ্কার কোলাবোরেশন এবং বেশি ক্যাম্পেইন ভলিউমেও ব্র্যান্ড-সম্মত আউটপুট বজায় রাখা সহজ হয়।',
      ],
      points: [
        {
          icon: 'sparkles',
          title: 'গার্ডরেইলসহ ক্রিয়েটিভ ইন্টেলিজেন্স',
          description: 'দ্রুত জেনারেট করুন, কিন্তু টোন, প্রোডাক্ট কনটেক্সট ও ব্র্যান্ড নির্দেশনা ঠিক রাখুন।',
        },
        {
          icon: 'users',
          title: 'টিম-রেডি ওয়ার্কফ্লো',
          description: 'মার্কেটিং, ডিজাইন ও অনুমোদন স্টেকহোল্ডারদের একই ফ্লোতে যুক্ত রাখুন।',
        },
        {
          icon: 'shield-check',
          title: 'অপারেশনাল নির্ভরযোগ্যতা',
          description: 'অ্যাসেট রিভিউ, এক্সপোর্ট সিদ্ধান্ত এবং প্ল্যাটফর্ম রেডিনেস সবার জন্য দৃশ্যমান রাখুন।',
        },
      ],
      stats: [
        { value: '10x', label: 'দ্রুততর কনসেপ্ট টার্নঅ্যারাউন্ড' },
        { value: '4', label: 'প্রধান পাবলিশিং চ্যানেল' },
        { value: '1', label: 'সংযুক্ত ক্রিয়েটিভ ওয়ার্কস্পেস' },
      ],
      primaryCta: 'ওয়ার্কফ্লো দেখুন',
      secondaryCta: 'প্রাইসিং দেখুন',
    },
    footer: 'LEBHAS - Create Ads Beyond Imagination. সর্বস্বত্ব সংরক্ষিত।',
  },
};

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [AuthDialogComponent, IconComponent, RouterLink, ThemeToggleComponent],
  templateUrl: './home.html',
  styleUrl: './home.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent {
  private readonly document = inject(DOCUMENT);
  private readonly router = inject(Router);
  private readonly auth = inject(CurrentUserStore);
  private readonly themeStore = inject(ThemeStore);

  protected readonly currentYear = new Date().getFullYear();
  protected readonly isAuthenticated = this.auth.isAuthenticated;
  protected readonly currentTheme = this.themeStore.theme;
  protected readonly loginModalOpen = signal(false);
  protected readonly menuOpen = signal(false);
  protected readonly language = signal<HomeLanguage>('en');

  protected readonly copy = computed(() => HOME_COPY[this.language()]);

  protected readonly navItems = computed<readonly NavItem[]>(() => [
    { label: this.copy().nav.home, href: '#home' },
    { label: this.copy().nav.features, href: '#features' },
    { label: this.copy().nav.pricing, href: '#pricing' },
    { label: this.copy().nav.resources, href: '#workflow', hasDropdown: true },
    { label: this.copy().nav.about, href: '#about' },
  ]);

  protected readonly heroHighlights = computed(() => this.copy().hero.highlights);
  protected readonly processingStages = computed(() => this.copy().hero.processingStages);
  protected readonly outcomeChips = computed(() => this.copy().hero.outcomes);

  protected readonly platformBadges = computed<readonly PlatformBadge[]>(() => [
    { name: 'Facebook', shortLabel: 'f', accentClass: 'platform-facebook' },
    { name: 'Instagram', shortLabel: 'ig', accentClass: 'platform-instagram' },
    { name: 'TikTok', shortLabel: 'tt', accentClass: 'platform-tiktok' },
    { name: 'LinkedIn', shortLabel: 'in', accentClass: 'platform-linkedin' },
    {
      name: this.copy().platformStrip.moreLabel,
      shortLabel: '...',
      accentClass: 'platform-more',
    },
  ]);

  protected readonly workflowSteps = computed(() => this.copy().workflow.steps);
  protected readonly workspaceMetrics = computed(() => this.copy().workspace.metrics);
  protected readonly activityItems = computed(() => this.copy().workspace.activities);
  protected readonly pricingPlans = computed(() => this.copy().pricing.plans);
  protected readonly aboutPoints = computed(() => this.copy().about.points);
  protected readonly aboutStats = computed(() => this.copy().about.stats);
  protected readonly topPlatforms = TOP_PLATFORMS;

  protected readonly donutGradient = computed(() => {
    let start = 0;

    const segments = this.topPlatforms.map((platform) => {
      const end = start + platform.share;
      const segment = `${platform.color} ${start}% ${end}%`;
      start = end;
      return segment;
    });

    return `conic-gradient(${segments.join(', ')})`;
  });

  constructor() {
    const searchParams = new URLSearchParams(globalThis.location.search);
    const openedFromLoginRoute = globalThis.location.pathname === '/login';
    const openedFromQuery = searchParams.get('auth') === 'login';

    this.loginModalOpen.set(openedFromLoginRoute || openedFromQuery);
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
}
