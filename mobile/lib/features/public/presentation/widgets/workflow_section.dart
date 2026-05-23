import 'package:flutter/material.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';

class WorkflowSection extends StatelessWidget {
  const WorkflowSection({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 64),
      child: Column(
        children: [
          Text(
            'The Lebhas Workflow',
            style: context.textTheme.displayMedium?.copyWith(
              fontWeight: FontWeight.bold,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 16),
          Text(
            'A seamless structure from organization to creative output.',
            style: context.textTheme.bodyLarge?.copyWith(
              color: context.colorScheme.onSurface.withValues(alpha: 0.7),
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 56),
          _WorkflowStep(
            number: '01',
            title: 'Workspace',
            description: 'Central hub for your team or agency organization.',
            icon: Icons.workspaces_outline,
          ),
          _WorkflowStep(
            number: '02',
            title: 'Brands',
            description: 'Manage individual brand identities and assets.',
            icon: Icons.branding_watermark_outlined,
          ),
          _WorkflowStep(
            number: '03',
            title: 'Products/Services',
            description: 'Organize by specific product lines or service offerings.',
            icon: Icons.inventory_2_outlined,
          ),
          _WorkflowStep(
            number: '04',
            title: 'Projects/Campaigns',
            description: 'Launch specific marketing initiatives and goals.',
            icon: Icons.campaign_outlined,
          ),
          _WorkflowStep(
            number: '05',
            title: 'Creative Versions',
            description: 'Generate and iterate on AI-powered creatives.',
            icon: Icons.auto_awesome_outlined,
          ),
          _WorkflowStep(
            number: '06',
            title: 'Approval & Delivery',
            description: 'Workflow for approval, sharing, and usage billing.',
            icon: Icons.verified_outlined,
            isLast: true,
          ),
        ],
      ),
    );
  }
}

class _WorkflowStep extends StatelessWidget {
  final String number;
  final String title;
  final String description;
  final IconData icon;
  final bool isLast;

  const _WorkflowStep({
    required this.number,
    required this.title,
    required this.description,
    required this.icon,
    this.isLast = false,
  });

  @override
  Widget build(BuildContext context) {
    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Column(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: context.colorScheme.primary.withValues(alpha: 0.1),
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: context.colorScheme.primary.withValues(alpha: 0.5),
                    width: 2,
                  ),
                ),
                child: Center(
                  child: Text(
                    number,
                    style: TextStyle(
                      color: context.colorScheme.primary,
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                    ),
                  ),
                ),
              ),
              if (!isLast)
                Expanded(
                  child: Container(
                    width: 2,
                    color: context.colorScheme.primary.withValues(alpha: 0.2),
                  ),
                ),
            ],
          ),
          const SizedBox(width: 24),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(bottom: 40),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(icon, size: 24, color: context.colorScheme.primary),
                      const SizedBox(width: 12),
                      Text(
                        title,
                        style: context.textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    description,
                    style: context.textTheme.bodyMedium?.copyWith(
                      color: context.colorScheme.onSurface.withValues(alpha: 0.7),
                      height: 1.5,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
