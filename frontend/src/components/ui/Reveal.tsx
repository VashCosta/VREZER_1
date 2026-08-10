import React, { useRef } from 'react';
import { motion, useInView, Variants } from 'framer-motion';

interface RevealProps {
  children: React.ReactNode;
  width?: "fit-content" | "100%";
  type?: "fade-up" | "scale" | "clip-path" | "stagger-container" | "stagger-item";
  delay?: number;
  duration?: number;
  className?: string;
  staggerChildren?: number;
}

export const Reveal = ({ 
  children, 
  width = "fit-content", 
  type = "fade-up", 
  delay = 0, 
  duration = 0.6,
  className = "",
  staggerChildren = 0.1
}: RevealProps) => {
  const ref = useRef(null);
  const isInView = useInView(ref, { once: true, margin: "-10%" });

  // power3.out equivalent easing in Framer Motion is [0.175, 0.885, 0.32, 1]
  // expo.out equivalent is [0.19, 1, 0.22, 1]
  const defaultEasing = [0.19, 1, 0.22, 1];

  const variants: Record<string, Variants> = {
    "fade-up": {
      hidden: { opacity: 0, y: 50 },
      visible: { 
        opacity: 1, 
        y: 0, 
        transition: { duration, delay, ease: defaultEasing } 
      }
    },
    "scale": {
      hidden: { opacity: 0, scale: 0.9 },
      visible: { 
        opacity: 1, 
        scale: 1, 
        transition: { duration, delay, ease: defaultEasing } 
      }
    },
    "clip-path": {
      hidden: { opacity: 0, clipPath: "inset(100% 0 0 0)" },
      visible: { 
        opacity: 1, 
        clipPath: "inset(0% 0 0 0)", 
        transition: { duration: duration * 1.2, delay, ease: defaultEasing } 
      }
    },
    "stagger-container": {
      hidden: { opacity: 0 },
      visible: {
        opacity: 1,
        transition: {
          staggerChildren,
          delayChildren: delay
        }
      }
    },
    "stagger-item": {
      hidden: { opacity: 0, y: 30 },
      visible: {
        opacity: 1,
        y: 0,
        transition: { duration, ease: defaultEasing }
      }
    }
  };

  if (type === "stagger-container") {
    return (
      <motion.div
        ref={ref}
        variants={variants[type]}
        initial="hidden"
        animate={isInView ? "visible" : "hidden"}
        className={className}
        style={{ width }}
      >
        {children}
      </motion.div>
    );
  }

  return (
    <div ref={ref} style={{ width, position: "relative" }} className={className}>
      <motion.div
        variants={variants[type]}
        initial="hidden"
        animate={isInView ? "visible" : "hidden"}
        className="h-full w-full"
      >
        {children}
      </motion.div>
    </div>
  );
};
